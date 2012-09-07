package de.dfki.asr.genesis2.sms.servlets;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.HttpHostConnectException;

import de.dfki.asr.genesis2.sms.util.FacebookPOIXMLExtractor;
import de.dfki.asr.genesis2.sms.util.FlickrPOIXMLExtractor;
import de.dfki.asr.genesis2.sms.util.Point;
import de.dfki.asr.genesis2.sms.util.SocialNetworkSource;
import de.dfki.asr.genesis2.sms.util.SocialPOIXMLExtractor;
import de.dfki.asr.genesis2.sms.util.TwitterPOIXMLExtractor;
import de.dfki.asr.genesis2.sms.util.YoutubePOIXMLExtractor;

/**
 * Servlet implementation class LocationSearch
 */
@WebServlet("/search")
public class LocationSearch extends HttpServlet {
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
	private static int defaultMaxResults;
	private static int defaultMaxRadiusInKm;
	private static String defaultRadiusInKm;
	
	private static HashSet<String> langSet;
	
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public LocationSearch() {
        super();  
        loadProperties();
    }
    
    private void loadProperties() {
        Properties props = new Properties();
        URL url;     
        InputStream in;
        try {
        	url = this.getClass().getClassLoader().getResource("/search.properties");
        	in = url.openStream();
			props.load(in);
			TWITTER_URL = props.getProperty("twitter-url");
			YOUTUBE_URL = props.getProperty("youtube-url");
			FLICKR_URL = props.getProperty("flickr-url");
			FACEBOOK_URL = props.getProperty("facebook-url");
			
			TWITTER_CONST_PARAMS = props.getProperty("twitter-const-params");
			YOUTUBE_CONST_PARAMS = props.getProperty("youtube-const-params");
			FLICKR_CONST_PARAMS = props.getProperty("flickr-const-params");
			FACEBOOK_CONST_PARAMS = props.getProperty("facebook-const-params");
			
			FLICKR_EXTRAS = props.getProperty("flickr-extras");
			FLICKR_API_KEY = props.getProperty("flickr-api-key");
			
			defaultMaxResults = Integer.parseInt(props.getProperty("dft-max-results"));
			defaultMaxRadiusInKm = Integer.parseInt(props.getProperty("dft-max-radius").replaceFirst("km", ""));
			defaultRadiusInKm = props.getProperty("dft-radius");
			
			url = this.getClass().getClassLoader().getResource("/lang.csv");
			in = url.openStream();
			BufferedReader br  = new BufferedReader(new InputStreamReader(in));
			String lang;
			langSet = new HashSet<String>();
			while((lang = br.readLine()) != null) {
				langSet.add(lang);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Get parameters from request
		String query = request.getParameter("q");
		String radius = request.getParameter("radius");
		String maxResults = request.getParameter("max-results");
		String location = request.getParameter("location");
		String sources = request.getParameter("sources");
		String language = request.getParameter("lang");
	
		//Check Query parameter
		if (query==null || query.equals("")) {
			response.sendError(HttpStatus.SC_BAD_REQUEST, "Query parameter missing, it's a mandatory non-empty parameter");
			return;
		}
		
		// Check Location parameter
		if (location==null || location.equals("")) {
			response.sendError(HttpStatus.SC_BAD_REQUEST, "Location parameter missing, it's a mandatory non-empty parameter");
			return;
		}
		else if (!Point.validLocation(location)) {
			response.sendError(HttpStatus.SC_BAD_REQUEST, "Invalid location parameter, it should be latitude[-180,180],longitude[-90,90]. Example: location=37.42307,-122.08427 ");
			return;
		}
		
		// Check Max-Results parameter
		if (maxResults==null || maxResults.equals("")) {
			maxResults = Integer.toString(defaultMaxResults);
		}
		else {
			if (!maxResults.matches("[0-9]+") || Integer.parseInt(maxResults) <= 0 || Integer.parseInt(maxResults) > 50) {
				response.sendError(HttpStatus.SC_BAD_REQUEST, "Invalid max-results parameter, it should be a positive integer not greater than 50 ");
				return;
			}
		}
		
		// Check Location-Radius parameter
		if (radius!=null && !radius.equals("")) {
			if (!radius.matches("[0-9]+(\\.[0-9]+)?(km)?")) {
				response.sendError(HttpStatus.SC_BAD_REQUEST, "Invalid radius parameter, it should contain a float distance in kilometers with optional unit. Example: radius=23.5km");
				return;
			}
			else {
				if (Double.parseDouble(radius.replaceFirst("km", "")) > defaultMaxRadiusInKm) {
					response.sendError(HttpStatus.SC_BAD_REQUEST, "Invalid radius parameter, it should not be greater than " + defaultMaxRadiusInKm);
				}
				// Youtube API doesn't accept radius parameter without distance unit
				if (radius.matches("[0-9]+(\\.[0-9]+)?")) {
					radius += "km";
				}
			}
		}
		else {
			radius = defaultRadiusInKm;
		}
		
		// Check Language parameter
		if (language != null && !language.equals("") && !langSet.contains(language)) {
			response.sendError(HttpStatus.SC_BAD_REQUEST, "Invalid language parameter, it should be in the format 'gg_CC' where gg = ISO 639 Language code, CC = ISO 3166-1 alpha-2 Country Code. Supported languages: https://www.facebook.com/translations/FacebookLocales.xml");
		}
		
		// Check Sources parameter
		boolean twitter = false, youtube = false, flickr = false, facebook = false;
		if (sources==null || sources=="") {
			twitter = true;
			youtube = true;
			flickr =true;
			facebook = true;
		}
		else {
			if (sources.contains("twitter")) twitter = true;
			if (sources.contains("youtube")) youtube = true;
			if (sources.contains("flickr"))  flickr  = true;
			if (sources.contains("facebook"))  facebook  = true;
			if (youtube==false && twitter==false && flickr==false && facebook==false) {
				response.sendError(HttpStatus.SC_BAD_REQUEST, "Invalid sources parameter, it should be a CSV subset of {twitter,youtube,facebook,flickr}. Example: sources=twitter ");
				return;
			}
		}
		
		
        HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());
		GetMethod get = null;
		query = query.replace(" ", "%20");
		int max = Integer.parseInt(maxResults);
		PrintWriter out = response.getWriter();

		out.write("<pois>\n");

		ArrayList<Thread> thread = new ArrayList<Thread>();
		try {	
			if (facebook) {
				String facebookURL = assemblefacebookRequest(query, location, radius, maxResults, language);
				get = new GetMethod(facebookURL);
				thread.add(new Thread(new GetThread(client, get, response, out, SocialNetworkSource.FACEBOOK, max)));
			}
			
			if (flickr) {
				String flickrURL = assembleFlickrRequest(query, location, radius, maxResults, language);
				get = new GetMethod(flickrURL);
				thread.add(new Thread(new GetThread(client, get, response, out, SocialNetworkSource.FLICKR, max)));
			}
			
			if (twitter) {
				String twitterURL = assembleTwitterRequest(query, location, radius, maxResults, language);
				get = new GetMethod(twitterURL);
				thread.add(new Thread(new GetThread(client, get, response, out, SocialNetworkSource.TWITTER, max)));
			}
	
			if (youtube) {	
				String youtubeURL = assembleYoutubeRequest(query, location, radius, maxResults, language);
				get = new GetMethod(youtubeURL);
				thread.add(new Thread(new GetThread(client, get, response, out, SocialNetworkSource.YOUTUBE, max)));
			}
			
			for (Thread t: thread) t.start();
			for (Thread t: thread) t.join();

		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
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

    
    private static String assembleYoutubeRequest(String query, String location, String radius, String maxResults, String language) {

    	String req = YOUTUBE_URL + "?" + YOUTUBE_CONST_PARAMS;
    	
    	query = query.replace("+", "%20");
    	
    	if (query!= null && query!="") {
    		req += "&q=" + query;
    	}
    	if (location!= null && location!="") {
    		req += "&location=" + location;
    	}
    	if (radius!= null && radius!="") {
    		req += "&location-radius=" + radius;
    	}
    	if (maxResults!= null && maxResults!="") {
    		req += "&max-results=" + maxResults;
    	}
    	if (language!= null && language!="") {
    		req += "&lr=" + language.substring(0, 2);
    	}
    	
    	return req;
    }
    
    private static String assembleTwitterRequest(String query, String location, String radius, String maxResults, String language) {

    	String req = TWITTER_URL + "?" + TWITTER_CONST_PARAMS;
    	
		String latlong[] = location.split(",");
		String lat = latlong[0];
		String lon = latlong[1];
    	
    	if (query!= null && query!="") {
    		req += "&q=" + query;
    	}
    	if (lat!= null && lat!="" && lon!= null && lon!="") {
    		req += "&lat=" + lat + "&long=" + lon;
    	}
    	if (radius!= null && radius!="") {
    		req += "&location-radius=" + radius;
    	}
    	if (maxResults!= null && maxResults!="") {
    		req += "&rpp=" + maxResults;
    	}
    	if (language!= null && language!="") {
    		req += "&lang=" + language.substring(0, 2);
    	}
    	
    	return req;
    }
    
    private static String assembleFlickrRequest(String query, String location, String radius, String maxResults, String language) {
    	
    	String req = FLICKR_URL + "?" + FLICKR_CONST_PARAMS
    							+ "&api_key=" + FLICKR_API_KEY 
    							+ "&extras=" + FLICKR_EXTRAS;
    	
		String latlong[] = location.split(",");
		String lat = latlong[0];
		String lon = latlong[1];
    	
    	if (query!= null && query!="") {
    		req += "&text=" + query;
    	}
    	if (lat!= null && lat!="" && lon!= null && lon!="") {
    		req += "&lat=" + lat + "&lon=" + lon;
    	}
    	if (radius!= null && radius!="") {
    		radius = radius.replace("km","");
    		if (Double.parseDouble(radius) > 20) 
    			radius = "20";
    		req += "&radius=" + radius;
    	}
    	if (maxResults!= null && maxResults!="") {
    		req += "&per_page=" + maxResults;
    	}
    	
    	return req;
    }

	private String assemblefacebookRequest(String query, String location, String radius, String maxResults, String language) {
    	String req = FACEBOOK_URL + "?" + FACEBOOK_CONST_PARAMS;
    	
    	// TODO : Replace for DB access when AuthenticationService is done, Facebook token expires in 2 months 
    	req += "&access_token=AAAFsBCj8RD4BAKdmOPqblkZBZC2wYZA19WefX8WaQO1XaJT0mZCmLoa33dQ0bIr3797R0JEECJjr2eYdihyiCQd2qeej5lpEwAho5eGCIgZDZD";
    	
    	if (query!= null && query!="") {
    		req += "&q=" + query;
    	}
    	if (location!= null && location!="") {
    		req += "&center=" + location;
    	}
    	if (radius!= null && radius!="") {
    		req += "&distance=" + radius;
    	}
    	if (maxResults!= null && maxResults!="") {
    		req += "&limit=" + maxResults;
    	}
    	if (language!= null && language!="") {
    		req += "&locale=" + language;
    	}
    	
    	return req;
	}	
	
	
	static class GetThread implements Runnable {

        private final HttpClient client;
        private final GetMethod get;
        private final SocialNetworkSource source;
        private final HttpServletResponse response;
        private final PrintWriter out;
        private final int maxResults;
		private static final SocialPOIXMLExtractor youtubeExtractor = new YoutubePOIXMLExtractor();
		private static final SocialPOIXMLExtractor twittwerExtractor = new TwitterPOIXMLExtractor(false);
		private static final SocialPOIXMLExtractor facebookExtractor = new FacebookPOIXMLExtractor();
		private static final SocialPOIXMLExtractor flickrExtractor = new FlickrPOIXMLExtractor();

        public GetThread(HttpClient client, GetMethod get, HttpServletResponse response, PrintWriter out, SocialNetworkSource source, int max) {
            this.client = client;
            this.get = get;
            this.source = source;
            this.response = response;
            this.out = out;
            this.maxResults = max;
		} 

        /**
         * Executes the GetMethod and prints some status information.
         */
        @Override
        public void run() {
            try {
            	client.executeMethod(get);         	
    			Thread.sleep(0,1);
    			//String body = get.getResponseBodyAsString();
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
    	    		switch (source) {
	    				case YOUTUBE: out.write(youtubeExtractor.extractPOIXMLsFromResponse(body, maxResults));
	    				case TWITTER: out.write(twittwerExtractor.extractPOIXMLsFromResponse(body, maxResults));
	    				case FACEBOOK: out.write(facebookExtractor.extractPOIXMLsFromResponse(body, maxResults));
	    				case FLICKR: out.write(flickrExtractor.extractPOIXMLsFromResponse(body, maxResults));
	        		}
    				Thread.sleep(0,1);
    				out.flush();
    		        response.flushBuffer();
    			}
    			else {
    				response.sendError(statusCode, body);
    				return;
    			}
    			
    		}
    		catch (HttpHostConnectException e) {
    			e.printStackTrace();
    		} catch (ClientProtocolException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

    }
	
	
    
}
