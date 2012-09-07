package de.dfki.asr.genesis2.sms.servlets;


import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;
import org.apache.jcs.access.exception.CacheException;
import org.json.JSONException;
import org.json.JSONObject;

import de.dfki.asr.genesis2.sms.cache.CacheObject;
import de.dfki.asr.genesis2.sms.cache.CacheQueue;
import de.dfki.asr.genesis2.sms.cache.SearchCache;
import de.dfki.asr.genesis2.sms.util.FacebookPOIXMLExtractor;
import de.dfki.asr.genesis2.sms.util.FlickrPOIXMLExtractor;
import de.dfki.asr.genesis2.sms.util.Point;
import de.dfki.asr.genesis2.sms.util.SocialNetworkSource;
import de.dfki.asr.genesis2.sms.util.SocialPOIXMLExtractor;
import de.dfki.asr.genesis2.sms.util.TwitterPOIXMLExtractor;
import de.dfki.asr.genesis2.sms.util.YoutubePOIXMLExtractor;

/**
 * Servlet implementation class LocationSearch
 * 
 * @author Andre de Oliveira Melo
 *
 */
@WebServlet("/cachedsearch")
public class CachedLocationSearch extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	private static String TWITTER_URL;
	private static String YOUTUBE_URL;
	private static String FLICKR_URL;
	private static String FACEBOOK_URL;
	private static String TWITTER_CONST_PARAMS;
	private static String YOUTUBE_CONST_PARAMS;
	private static String FLICKR_CONST_PARAMS;
	private static String FACEBOOK_CONST_PARAMS;
	private static String FLICKR_EXTRAS;
	private static String FLICKR_API_KEY;
	private static long TWITTER_REFRESH_INTERVAL;
	private static long YOUTUBE_REFRESH_INTERVAL;
	private static long FLICKR_REFRESH_INTERVAL;
	private static long FACEBOOK_REFRESH_INTERVAL;
	private static int defaultMaxResults;
	private static int defaultMaxRadiusInKm;
	private static HashSet<String> langSet;
	
	private static final SearchCache cache = new SearchCache("/cache.ccf");
	
	private static final Logger logger = Logger.getLogger("handlers");
	
	private class RequestParameters {
		private String query = null;
		private Point location = null;
		private String language = null;
		private float radius = -1;
		private int maxResults = -1;
		private String sinceId = null;
		private long sinceTS = -1;
		private boolean twitter = false;
		private boolean youtube = false;
		private boolean flickr = false;
		private boolean facebook = false;
		
		/**
		 * Sets the query parameter and checks if it's legal
		 * @param query
		 * 		A string containing the query keywords
		 * @throws IllegalArgumentException
		 * 		if query is null or empty
		 */
		public void setQuery(String query) throws IllegalArgumentException {
			if (query==null || query.equals(""))
				throw new IllegalArgumentException("Query parameter missing, it's a mandatory non-empty parameter");	
			try {
				this.query = URLEncoder.encode(query, "UTF-8");
			} catch (UnsupportedEncodingException e) {}
		}	
		/**
		 * Sets the location parameter and checks if it's legal
		 * @param location
		 * 		A point coordinate in the format latitude,longitude
		 * @throws IllegalArgumentException
		 * 		if location is not in the correct format: latitude,longitude
		 *  	if location is null or empty
		 *  
		 */
		public void setLocation(String location) throws IllegalArgumentException {
			if (location==null || location.equals("")) 
				throw new IllegalArgumentException("Location parameter missing, it's a mandatory non-empty parameter");		
			this.location = Point.parsePoint(location);
			if (this.location==null || !this.location.isValid())
				throw new IllegalArgumentException("Invalid parameter: location="+location+", it should be latitude[-180,180],longitude[-90,90]. Example: location=37.42307,-122.08427 ");
		}
		/**
		 * Sets the radius parameter and checks if it's legal. If empty or null, assigns default value
		 * @param radius
		 * 		A decimal number (in km) which together with the location will determine the search area
		 * @throws IllegalArgumentException
		 * 		if radius is not in the correct format: positive float with optional 'km' at the end specifying distance unit
		 */
		public void setRadius(String radius) throws IllegalArgumentException {
			try {
				if (radius!=null && !radius.equals("")) {
					float parsedRadius = Float.parseFloat(radius.replaceFirst("km", ""));
					this.radius = Math.min(parsedRadius,defaultMaxRadiusInKm);
				}
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid parameter: radius="+radius+", it should contain a float distance in kilometers with optional unit. Example: radius=23.5km");
			}
			
		}
		/**
		 * Sets the max-results parameter and checks if it's legal. If empty or null, assigns default value
		 * @param maxResults
		 * 		The maximum number of entries to be returned in the response
		 * @throws IllegalArgumentException
		 * 		if maxResults is not in the correct format: positive integer not greater than 50
		 */
		public void setMaxResults(String maxResults) throws IllegalArgumentException {
			if (maxResults==null || maxResults.equals("")) 
				this.maxResults = defaultMaxResults;
			else 
				try {
					int max = Integer.parseInt(maxResults);
					if (max>0 && max<=50)
						this.maxResults = max;
				}
				catch(NumberFormatException e) {
					throw new IllegalArgumentException("Invalid parameter: max-results="+maxResults+", it should be a valid positive not greater than 50");
				}
		}
		/** 
		 * Sets the language parameter and checks if it's legal
		 * @param language
		 * 		A string encoding the search language with the format gg_CC (gg = language code, CC = country code, example: en_US)
		 * @throws IllegalArgumentException
		 * 		if language is not valid: check https://www.facebook.com/translations/FacebookLocales.xml to see 
		 */
		public void setLanguage(String language) throws IllegalArgumentException {
			if (language != null && !language.equals("")) {
				if (!langSet.contains(language)) 
					throw new IllegalArgumentException("Invalid parameter: language="+language+", it should be in the format 'gg_CC' where gg = ISO 639 Language code, CC = ISO 3166-1 alpha-2 Country Code. Supported languages: https://www.facebook.com/translations/FacebookLocales.xml");
				else
					this.language = language;
			}
		}
		/**
		 * Sets the sources parameter and checks if it's legal. If null or empty all sources are set to true
		 * @param sources
		 * 		A string containing the set of sources (separated by comma) to be returned in the response
		 * @throws IllegalArgumentException
		 * 		If a non-empty source doesn't match any of the available sources' names
		 */
		public void setSources(String sources) throws IllegalArgumentException {
			if (sources==null || sources=="")
				twitter = youtube = flickr = facebook = true;
			else {
				if (sources.contains("twitter")) twitter = true;
				if (sources.contains("youtube")) youtube = true;
				if (sources.contains("flickr"))  flickr  = true;
				if (sources.contains("facebook"))  facebook  = true;
				if (youtube==false && twitter==false && flickr==false && facebook==false) 
					throw new IllegalArgumentException("Invalid parameter sources="+sources+", it should be a CSV subset of {twitter,youtube,facebook,flickr}. Example: sources=twitter");
			}
		}
		/**
		 * Sets the sinceId (Specific for Twitter which doesn't accept sinceTimestamp)
		 * @param sinceId
		 * 		Constrains the response results to Tweets later than the one identified by sinceID 
		 */
		public void setSinceId(String sinceId){ 
			this.sinceId = sinceId; 
		}
		/**
		 * Sets the sinceTimestamp
		 * @param sinceTimestamp
		 * 		Constrains the response results to events occurred after the sinceTimestamp 
		 */
		public void setSinceTimestamp(long sinceTimestamp){ 
			this.sinceTS = sinceTimestamp; 
		}
		
		public boolean isFacebookInSources() { return facebook; }
		public boolean isTwitterInSources() { return twitter; }
		public boolean isYoutubeInSources() { return youtube; }
		public boolean isFlickrInSources() { return flickr; }
		public int getMaxResults() { return maxResults; }
		public Point getLocation() { return location; }
		public String getQuery() { return query; }
		

		/**
		 * Assembles Youtube API request for the class parameters
		 * 
		 * @return the Youtube request URL string
		 */
	    public String getYoutubeRequest() {
	    	String req = YOUTUBE_URL + "?" + YOUTUBE_CONST_PARAMS;	    	
	    	if (query!=null)  	req += "&q=" + query;
	    	if (location!=null) req += "&location=" + location.toString();
		    if (radius>0)  		req += "&location-radius=" + radius + "km";
	    	if (maxResults>0) 	req += "&max-results=" + maxResults;
	    	if (language!=null) req += "&lr=" + language.substring(0, 2);
	    	return req;
	    }	    
	    
		/**
		 * Assembles Twitter API request for the class parameters
		 * 
		 * @return the Twitter request URL string
		 */
	    public String getTwitterRequest() {
	    	String req = TWITTER_URL + "?" + TWITTER_CONST_PARAMS;	    	
	    	if (query!=null)  	req += "&q=" + query;
	    	if (location!=null) req += "&lat=" + location.getLatitude() + "&long=" + location.getLongitude();
		    if (radius>0)  		req += "&location-radius=" + radius;
	    	if (maxResults>0) 	req += "&max-results=" + maxResults;
	    	if (language!=null) req += "&lang=" + language.substring(0, 2);
	    	if (sinceId!=null) 	req += "&since_id=" + sinceId;	    	
	    	return req;
	    }	    
	    
		/**
		 * Assembles Flickr API request for the class parameters
		 * 
		 * @return the Flickr request URL string
		 */
	    public String getFlickrRequest() {	    	
	    	String req = FLICKR_URL + "?" + FLICKR_CONST_PARAMS + "&api_key=" + FLICKR_API_KEY + "&extras=" + FLICKR_EXTRAS;    	
	    	if (query!=null)  	req += "&text=" + query;
	    	if (location!=null) req += "&lat=" + location.getLatitude() + "&lon=" + location.getLongitude();
		    if (radius>0)  		req += "&radius=" + Math.min(20, this.radius); // Flickr's maximum radius is 20km
	    	if (maxResults>0) 	req += "&per_page=" + maxResults;
	    	if (language!=null) req += "&locale=" + language;
	    	if (sinceTS > 0)	req += "&min_upload_date=" + sinceTS/1000; // Flickr's timestamp is in seconds, not milliseconds	    	
	    	return req;
	    }

		/**
		 * Assembles Facebook API request for the class parameters
		 * 
		 * @return the Facebook request URL string
		 */
	    public String getFacebookRequest() {
			String req = FACEBOOK_URL + "?" + FACEBOOK_CONST_PARAMS;		
	    	// TODO : Replace for DB access when AuthenticationService is done, Facebook token expires in 2 months 
	    	req += "&access_token=AAAFsBCj8RD4BAKdmOPqblkZBZC2wYZA19WefX8WaQO1XaJT0mZCmLoa33dQ0bIr3797R0JEECJjr2eYdihyiCQd2qeej5lpEwAho5eGCIgZDZD";	    	    	
	    	if (query!=null)  	req += "&q=" + query;
	    	if (location!=null) req += "&center=" + location;
		    if (radius>0)  		req += "&distance=" + radius;
	    	if (maxResults>0) 	req += "&limit=" + maxResults;
	    	if (language!=null) req += "&locale=" + language;
	    	if (sinceTS > 0) 	req += "&since=" + Long.toString(sinceTS/1000); // Facebook's timestamp is in seconds, not milliseconds
	    	return req;
		}	
	}
	
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CachedLocationSearch() {
        super();  
        loadProperties();           
        try {
            URL url = this.getClass().getClassLoader().getResource("/logging.properties");
			LogManager.getLogManager().readConfiguration(url.openStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * 
     */
    private void loadProperties() {
        Properties props = new Properties();
        URL url = this.getClass().getClassLoader().getResource("/search.properties");     
        InputStream in;
        try {
        	in = url.openStream();
			props.load(in);
			
			// The base URLs
			TWITTER_URL  = props.getProperty("twitter-url");
			YOUTUBE_URL  = props.getProperty("youtube-url");
			FLICKR_URL   = props.getProperty("flickr-url");
			FACEBOOK_URL = props.getProperty("facebook-url");
			
			// The parameters which are constant
			TWITTER_CONST_PARAMS  = props.getProperty("twitter-const-params");
			YOUTUBE_CONST_PARAMS  = props.getProperty("youtube-const-params");
			FLICKR_CONST_PARAMS   = props.getProperty("flickr-const-params");
			FACEBOOK_CONST_PARAMS = props.getProperty("facebook-const-params");
			
			// The time minimum time betwen two similar requests (same location and query)
			TWITTER_REFRESH_INTERVAL  = 1000*Long.parseLong(props.getProperty("twitter-refresh-interval"));
			YOUTUBE_REFRESH_INTERVAL  = 1000*Long.parseLong(props.getProperty("youtube-refresh-interval"));
			FLICKR_REFRESH_INTERVAL   = 1000*Long.parseLong(props.getProperty("flickr-refresh-interval"));
			FACEBOOK_REFRESH_INTERVAL = 1000*Long.parseLong(props.getProperty("facebook-refresh-interval"));
			
			// Flickr extra fields to be returned at response
			FLICKR_EXTRAS  = props.getProperty("flickr-extras");
			// Flickr API Key required whn using the API
			FLICKR_API_KEY = props.getProperty("flickr-api-key");
			
			defaultMaxResults = Integer.parseInt(props.getProperty("dft-max-results"));
			defaultMaxRadiusInKm = Integer.parseInt(props.getProperty("dft-max-radius").replaceFirst("km", ""));
			props.getProperty("dft-radius");
			
			url = this.getClass().getClassLoader().getResource("/lang.csv");
			in = url.openStream();
			BufferedReader br  = new BufferedReader(new InputStreamReader(in));
			String lang;
			langSet = new HashSet<String>();
			while((lang = br.readLine()) != null) {
				langSet.add(lang);
			}
		} catch (IOException e) {
			logger.severe("Could not open Properties File: " + url.getPath());
			e.printStackTrace();
		}
    }
    

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Gets parameters from request
		RequestParameters reqparams = new RequestParameters();
		try {
			reqparams.setQuery(request.getParameter("q"));
			reqparams.setRadius(request.getParameter("radius"));
			reqparams.setMaxResults(request.getParameter("max-results"));
			reqparams.setLocation(request.getParameter("location"));
			reqparams.setSources(request.getParameter("sources"));
			reqparams.setLanguage(request.getParameter("lang"));
		} catch (IllegalArgumentException e) {
			logger.severe("StatusCode=" + HttpStatus.SC_BAD_REQUEST + " Request=" + request.getRequestURI() + "\nMsg=" + e.getMessage());
			response.sendError(HttpStatus.SC_BAD_REQUEST, e.getMessage());
			return;
		}
	
		// Prepares response
        HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());
        response.setContentType("text/xml");
		PrintWriter out = response.getWriter();
		out.write("<pois>\n");
		
		// Creates one thread for each request source to query cache, source API and process the response
		ArrayList<Thread> thread = new ArrayList<Thread>();
		try {	
			if (reqparams.isTwitterInSources()) thread.add(new Thread(new GetThread(client, response, out, SocialNetworkSource.FACEBOOK, reqparams)));
			if (reqparams.isTwitterInSources()) thread.add(new Thread(new GetThread(client, response, out, SocialNetworkSource.TWITTER, reqparams)));
			if (reqparams.isYoutubeInSources()) thread.add(new Thread(new GetThread(client, response, out, SocialNetworkSource.YOUTUBE, reqparams)));
			if (reqparams.isFlickrInSources()) thread.add(new Thread(new GetThread(client, response, out, SocialNetworkSource.FLICKR, reqparams)));
			
			for (Thread t: thread) t.start();
			for (Thread t: thread) t.join();
		}
		catch (InterruptedException e) {
			logger.severe(e.getMessage());
			e.printStackTrace();
		}
			
		out.write("</pois>");
		out.flush();
        response.flushBuffer();
	}


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}
	
	/**
	 * 
	 * @author Andre de Oliveira Melo
	 *
	 */
	static class GetThread implements Runnable {

		private static final int truncator = 2; // Number of decimal digits to be truncated 
		
		private static long twitterRetryAfter = -1;
		private static long twitterRateLimited = -1;
		private static long facebookRetryAfter = -1;
		private static long facebookRateLimited = -1;
		private static long youtubeRetryAfter = -1;
		private static long youtubeRateLimited = -1;
		private static long flickrRetryAfter = -1;
		private static long flickrRateLimited = -1;
		
        private final HttpClient client;
        private final HttpServletResponse response;
        private final SocialNetworkSource source;
        private final PrintWriter out;
        private final RequestParameters params;
        private String key;
        private String lockerKey;
        private Object syncObj;
        private long refreshInterval;
        private long retryAfter = -1;
		private long rateLimited = -1;
        private SocialPOIXMLExtractor extractor;
        
        // To avoid sending multiple identical requests while first response doesn't arrive
        private static final HashMap<String,Object> lockedKeys = new HashMap<String,Object>();
        
		private static final SocialPOIXMLExtractor youtubeExtractor = new YoutubePOIXMLExtractor();
		private static final SocialPOIXMLExtractor twitterExtractor = new TwitterPOIXMLExtractor(false);
		private static final SocialPOIXMLExtractor facebookExtractor = new FacebookPOIXMLExtractor();
		private static final SocialPOIXMLExtractor flickrExtractor = new FlickrPOIXMLExtractor();

		/**
		 * 
		 * @param client
		 * 		The HttpClient that will execute this requests
		 * @param response
		 * 		The doGet response to the user (required to perform early flush)
		 * @param out
		 * 		The Response output to which results will be written
		 * @param source
		 * 		The Social Network source: (facebook, twitter, youtube or flickr)
		 * @param params
		 * 		The RequestParameters class with validated parameters 
		 */
        public GetThread(HttpClient client, HttpServletResponse response, PrintWriter out, SocialNetworkSource source, RequestParameters params) {
            this.client = client;
            this.response = response;
            this.source = source;
            this.out = out;
            this.params = params;
            
         	// truncates coordinates to abstract differences smaller than 10^-truncator
            Point p = params.getLocation();
            p.truncate(truncator);
            this.key = params.getQuery() + ":" + p.toString();
            if (params.language!=null) 
            	this.key = this.key + ":" + params.language;
            this.lockerKey = source + ":" + this.key;
            
            switch (this.source) {
				case FACEBOOK: 
					this.refreshInterval = FACEBOOK_REFRESH_INTERVAL;
					this.extractor = facebookExtractor;
					this.rateLimited = facebookRateLimited;
					this.retryAfter = facebookRetryAfter;
					break;
				case YOUTUBE: 
					this.refreshInterval = YOUTUBE_REFRESH_INTERVAL;
					this.extractor = youtubeExtractor;
					this.rateLimited = youtubeRateLimited;
					this.retryAfter = youtubeRetryAfter;
					break;
				case TWITTER: 
					this.refreshInterval = TWITTER_REFRESH_INTERVAL;
					this.extractor = twitterExtractor;
					this.rateLimited = twitterRateLimited;
					this.retryAfter = twitterRetryAfter;
					break;	    				
				case FLICKR: 
					this.refreshInterval = FLICKR_REFRESH_INTERVAL;
					this.extractor = flickrExtractor;
					this.rateLimited = flickrRateLimited;
					this.retryAfter = flickrRetryAfter;
					break;
            }
		} 

        /**
         * Checks if unsuccessful response is because of rate limitation
         * @param body
         * 		The response body
         * @param get
         * 		The already executed get
         */
        public void checkRateLimit(String body,GetMethod get) { 
			int statusCode = get.getStatusCode();
        	long now = System.currentTimeMillis();
        	if (source==SocialNetworkSource.TWITTER && statusCode==420) {
				twitterRateLimited = now;
				twitterRetryAfter = 1000*Long.parseLong(get.getResponseHeader("Retry-After").getValue());		    					
			}
			if (source==SocialNetworkSource.FACEBOOK && statusCode==400) {
				try {
					JSONObject jobj = new JSONObject(body);
					if (jobj.getJSONObject("error").getInt("code") == 613) {
						facebookRateLimited = now;
						facebookRetryAfter = 1000*120;
					}
				} catch (JSONException e) {}	    					
			}
			if (source==SocialNetworkSource.YOUTUBE && statusCode==403) {
				if (body.contains("too_many_recent_calls")) {
					youtubeRateLimited = now;
					youtubeRetryAfter = 1000*120;
				}    					
			}
			// Flickr doesn't send any Rate Limit message
       	
        }
        
        @Override
        public void run() {
		
			long lastUpdate, timeDiff, sendRequestTS, waitedToRetry;
			CacheQueue<String> pois;
			try {
				
				// Checks if similar request was already sent, if not create an object for locking possible similar subsequent requests
				syncObj = lockedKeys.get(lockerKey);
	        	if (syncObj == null) {
					syncObj = new Object();
					lockedKeys.put(lockerKey, syncObj);
	        	}
	        	else {
	        		logger.info("Request for " + this.lockerKey + " was already sent and the response was still not received");
	        	}
	        	
	        	// Synchronization to avoid sending multiple similar requests while the first was not responded (likely to happen for slow APIs like facebook and flickr)
				synchronized(syncObj) {
					
					long cacheT1 = System.currentTimeMillis();
					pois = cache.get(key, source);
					logger.finest("CacheQueryTime="+(System.currentTimeMillis()-cacheT1));
					if (pois == null || pois.isEmpty()) {
						lastUpdate = -1;
						pois = new CacheQueue<String>();
					}
					else {
						lastUpdate = pois.getLastUpdate();
					}
					sendRequestTS = System.currentTimeMillis() ;
					timeDiff = sendRequestTS - lastUpdate;	
					waitedToRetry = sendRequestTS - rateLimited;

					// Checks time between current and latest similar request to avoid flooding
					if (timeDiff > refreshInterval) {
						// In case it was rate limited and suspension time is not over, don't query the source (respond only with cached data) 
						if (waitedToRetry <= retryAfter) {
							logger.warning(source + " was rate limited!");
						}
						else {
		    	    		String url = "";
		    	    		params.setSinceTimestamp(lastUpdate);
		    	    		params.setSinceId(pois.getMaxId());	// SinceId necessary for Twitter API which requires SinceId instead of Since Timestamp
							switch (source) {
		    					case FACEBOOK: url = params.getFacebookRequest(); break;
			    				case YOUTUBE: url = params.getYoutubeRequest(); break;
			    				case TWITTER: url = params.getTwitterRequest(); break;	    				
			    				case FLICKR: url = params.getFlickrRequest(); break;
			        		}
							logger.finest(url);
							GetMethod get = new GetMethod(url);
							client.executeMethod(get);         	
			    			Thread.sleep(0,1);
			    			String body = "";
			    			InputStream is = get.getResponseBodyAsStream();
			    			BufferedReader bf = new BufferedReader(new InputStreamReader(is));
			    			String line;
			    			Thread.sleep(0,1);
			    			while ((line=bf.readLine())!= null) {
			    				body += line;
			    				Thread.sleep(0,1);
			    			}
			    			int statusCode = get.getStatusCode();
			    			if (statusCode == HttpStatus.SC_OK) {
			    	    		pois.addAll(extractor.getCacheObjsFromResponse(body));
			    	    		pois.setLastUpdate(sendRequestTS);
			    	    		cache.put(key, pois, source);			
			    	    		
			    	    		if (rateLimited > 0) {
			    	    			if (source==SocialNetworkSource.TWITTER) twitterRateLimited = twitterRetryAfter = -1;
				    	    		if (source==SocialNetworkSource.FACEBOOK) facebookRateLimited = facebookRetryAfter = -1;
				    	    		if (source==SocialNetworkSource.YOUTUBE) youtubeRateLimited = youtubeRetryAfter = -1;
			    	    		}
			    			}
			    			else {
			    				logger.severe("StatusCode=" + statusCode + " from request: " + url + "\nBody=" + body);  
			    				// In case Twitter has rate limited the server save Retry-After and Timestamp from when it was rate limited
			    				checkRateLimit(body,get);
			    				return;
			    			}
						}
	
					}		
	
					lockedKeys.remove(lockerKey);
					
		    		Iterator<CacheObject<String>> iterator = pois.iterator();	
		    		int count = params.getMaxResults();
					while (iterator.hasNext() && pois.size() > count) {
						count++;
						iterator.next();
					}
					
					synchronized (client) {
						while (iterator.hasNext())  {
							out.write(iterator.next().getObject());
						}
						out.flush();
						response.flushBuffer();
					}

				}	
				

    		}
    		catch ( IOException e) {
    			logger.log(Level.SEVERE, e.getMessage());
    			e.printStackTrace();
    		} 
			catch (InterruptedException e) {
    			logger.log(Level.SEVERE, e.getMessage());
    			e.printStackTrace();
			}
			catch (CacheException e) {
    			logger.log(Level.SEVERE, e.getMessage());
    			e.printStackTrace();
			}
			catch (IllegalArgumentException e) {
    			logger.log(Level.SEVERE, e.getMessage());
    			e.printStackTrace();
			}
        }
	
	}
    
}
