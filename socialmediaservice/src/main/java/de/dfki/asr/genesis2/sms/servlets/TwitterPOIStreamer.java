package de.dfki.asr.genesis2.sms.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import de.dfki.asr.genesis2.sms.util.MapTile;
import de.dfki.asr.genesis2.sms.util.Point;
import de.dfki.asr.genesis2.sms.util.SocialPOIXMLExtractor;
import de.dfki.asr.genesis2.sms.util.TwitterPOIXMLExtractor;

/**
 * Servlet implementation class ThreadedServlert
 * 
 * @author Andre de Oliveira Melo
 *
 */
@WebServlet("/stream")
public class TwitterPOIStreamer extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static Object sem = new Object();
	
	//Buffers
    private static String lines[] = null;
    private static Point points[] = null;
    private static String poiXMLs[] = null;
    
    private static long tweetCounter = 0;
    
    private static Thread producerThread = null;
    private static HttpClient client;
    private static HttpGet get;
	private static BufferedReader bf;
	private static SocialPOIXMLExtractor poixmlExtractor;
	
	private static String RESOURCE_URL;
	private static String TWITTER_USERNAME;
	private static String TWITTER_PASSWORD;
	private static int BUFFER_SIZE;
	
	private static final Logger logger = Logger.getLogger("handlers");
	
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public TwitterPOIStreamer() {
        super();
        
        // Initializes the logger
        URL url = this.getClass().getClassLoader().getResource("/logging.properties");
        try {          
			LogManager.getLogManager().readConfiguration(url.openStream());			
		} catch (IOException e) {
			logger.severe("Error loading properties file " + url.getPath() + ".\n" + e.getMessage());
			e.printStackTrace();
		}   
        
        // Loads properties and initliazes the variables dependent on them
        loadProperties();     
        lines = new String[BUFFER_SIZE];
        points = new Point[BUFFER_SIZE];
        poiXMLs = new String[BUFFER_SIZE];
		get = new HttpGet(RESOURCE_URL);
		String loginData = TWITTER_USERNAME + ":" + TWITTER_PASSWORD;
		String auth = new String(Base64.encodeBase64(loginData.getBytes()));
		get.addHeader("Authorization", auth); 
		
		// Starts the producer thread that reads the Tweet stream
        poixmlExtractor = new TwitterPOIXMLExtractor(true);
		client = new DefaultHttpClient();
        if (producerThread==null || !producerThread.isAlive()) {
        	producerThread = new Thread(new TweetReader());
        	producerThread.start();
        }
        
    }
    
    private static void loadProperties() {
    	
        Properties props = new Properties();
        URL url = TwitterPOIStreamer.class.getClassLoader().getResource("/twitter.properties");
        //InputStream in = TwitterPOIStreamer.class.getClassLoader().getResourceAsStream("twitter.properties");
        try {
			props.load(url.openStream());
	        RESOURCE_URL = props.getProperty("resource_url");
	        TWITTER_USERNAME = props.getProperty("username");
	        TWITTER_PASSWORD = props.getProperty("password");
	        BUFFER_SIZE = Integer.parseInt(props.getProperty("buffer_size"));
		} catch (IOException e) {
			logger.severe("Error loading properties file " + url.getPath() + ".\n" + e.getMessage());
			e.printStackTrace();
		}
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String tilesString = request.getParameter("tiles");	
		ArrayList<MapTile> tiles = MapTile.extractTiles(tilesString);
		
		response.setContentType("text/event-stream; charset=utf-8");
	    response.setHeader("pragma", "no-cache,no-store");  
        response.setHeader("cache-control", "no-cache,no-store,max-age=0,max-stale=0");  
	    PrintWriter out = response.getWriter();
		
		long ccount = tweetCounter;
		while (true) {
             synchronized (sem) {
                 try {
                     sem.wait();
                     while(ccount < tweetCounter) {
                    	 ccount++;
                    	 int bufferPos = (int) (ccount % BUFFER_SIZE); 
                    	 if (points[bufferPos]!=null && tiles!=null && points[bufferPos].inMapTiles(tiles)) {                       	
                    		 String post = "event:msg\ndata:" +  poiXMLs[bufferPos] + "\n\n";
                    		 out.print(post);              
                        	 out.flush();
        			         response.flushBuffer();
                         }
                     }			    
                 } catch (InterruptedException e) {
                	 logger.severe("Consumer Thread Interrupted: " + e.getMessage());
                     Thread.interrupted();
                 }
             }
         }
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}
	
	
	/**
	 * Producer thread that read the Tweet stream and loads the extracted tweets into a buffer to be consumed by the Get methods
	 * 
	 * @author Andre de Oliveira Melo
	 *
	 */
    private static class TweetReader implements Runnable {
    	 
    	public TweetReader() {
			try {
				HttpResponse resp = client.execute(get);
				HttpEntity entity = resp.getEntity(); 		
	    		InputStream is = entity.getContent();
	    		bf = new BufferedReader(new InputStreamReader(is, "UTF8"));
	    		tweetCounter = 0;
			} catch (IOException e) {
				logger.severe("Error opening response stream from request: " + get.getURI() + "\n" + e.getMessage());
				e.printStackTrace();
			}
    	}
	
		/**
		 * Reads the next line from the stream, loads into the buffer and notify consumers
		 */
        public void produce() {
        	String line = null;
        	try {       	
        		line = bf.readLine();
				JSONObject jsonObj = new JSONObject(line);
				if (!jsonObj.has("limit")) {
					tweetCounter++;
					int bufferPos = (int) (tweetCounter % BUFFER_SIZE);
					lines[bufferPos] = line;
					points[bufferPos] = poixmlExtractor.extractPoint(jsonObj);
					poiXMLs[bufferPos] = poixmlExtractor.extractPOIXML(jsonObj);
					sem.notifyAll();
				}
				else { // In case line is about missed Tweets, log it
					logger.warning("Missed tweets from firehose = " + jsonObj.getJSONObject("limit").getInt("track") + " out of " + tweetCounter);
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				logger.severe("Error Processing the following JSONObject: " + line + "\n" + e.getMessage());		
				e.printStackTrace();
			}
            
        }

        public void run() {
        	while (true) {
                synchronized (sem) {
                    produce();					                   
                }
				try {
					Thread.sleep(0,1);
				} catch (InterruptedException e) {
					logger.severe("Producer Thread Interrupted: " + e.getMessage());
					e.printStackTrace();
				}
            }
        }
	}
    
    

}
