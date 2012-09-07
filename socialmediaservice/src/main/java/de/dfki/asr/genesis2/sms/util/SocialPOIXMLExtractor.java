package de.dfki.asr.genesis2.sms.util;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import de.dfki.asr.genesis2.sms.cache.CacheObject;
import de.dfki.asr.genesis2.sms.cache.CacheQueue;

/**
 * SocialPOIXMLExtractor Class
 * 
 * This abstract class is responsible for setting the abstract methods for extracting information for the sources
 * and assemble the extract information as a SocialPOIXML object.
 * Each source extension of this class will be responsible for overcoming the differences between information
 * obtained with the APIs and the standardized structure defined by SocialPOIXML.
 * 
 * @author Andre de Oliveira Melo
 *
 */
public abstract class SocialPOIXMLExtractor {	
	
	private static final Logger logger = Logger.getLogger("handlers");
	
	public SocialPOIXMLExtractor(){
	    URL url = this.getClass().getClassLoader().getResource("/logging.properties");
		try {
			LogManager.getLogManager().readConfiguration(url.openStream());
		} catch (SecurityException e ) {
			e.printStackTrace();
		} catch  (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Parses JSON object and gathers the extracted information and organizes it in a SocialPOIXML object
	 * @param jsonObj
	 * 		JSON object with the information to be extracted
	 * @return
	 * 		SocialPOIXML object with XStream tags for XML conversion
	 */
	public SocialPOIXML parseJSON(JSONObject jsonObj) {
		
		Location location = extractLocation(jsonObj);
		SocialEvent socialEvent = new SocialEvent();
		
		try {		
			socialEvent.setFrom(extractFrom(jsonObj));
			socialEvent.setKeywords(extractKeywords(jsonObj));
			
			socialEvent.setTitle(extractLabel(jsonObj));
			socialEvent.setMessage(extractMessage(jsonObj));
			socialEvent.setDescription(extractDescription(jsonObj));
			
			socialEvent.setTagList(extractTagList(jsonObj));
			socialEvent.setToList(extractToList(jsonObj));
			
			socialEvent.setMediaContent(extractMediaContent(jsonObj));
			socialEvent.setMediaThumbnail(extractMediaThumbnail(jsonObj));
			
			SocialPOIXML poi = new SocialPOIXML();
			poi.setId(extractId(jsonObj));
			//poi.setLabel(extractLabel(jsonObj));
			poi.setLabel(extractSource(jsonObj));
			//poi.setDescription(extractDescription(jsonObj));
			poi.setLink(extractLink(jsonObj));
			poi.setTime(extractTimestamp(jsonObj));
			poi.setLocation(location);
			poi.setSocialEvent(socialEvent);

			return poi;
		} catch (JSONException e) {
			logger.severe("JSONException: "+e.getMessage()+"\nIn JSONObject from"+socialEvent.getSource()+": "+jsonObj.toString());
			e.printStackTrace();
			return null;
		}
		
		
	}
	
	/**
	 * Parses JSON object and converts into a POIXML string
	 * @param jsonObj
	 * 		JSON object with the information to be extracted
	 * @return
	 * 		XML String with the extracted information
	 */
	public String extractPOIXML(JSONObject jsonObj) { 
		return parseJSON(jsonObj).toString();
	}
	
	/**
	 * Extracts POIs in XML format from response body, up to a maximum number of POIs 
	 * @param body
	 * 		Response body string in JSON format
	 * @param maxReults
	 * 		Maximum number of POIs to be returned
	 * @return
	 * 		String with the list of extracted POIs in XML
	 */
	public String extractPOIXMLsFromResponse(String body, int maxReults) {
		String out = "";
		CacheQueue<String> list;
		list = this.getCacheObjsFromResponse(body);
		int count = 0;
		for (CacheObject<String> poi: list) {
			out += poi.getObject();
			if (++count >= maxReults)
				break;
		}
		return out;
	}
	/**
	 * Extracts POIs in XML format from response body
	 * @param body
	 * 		Response body string in JSON format
	 * @return
	 * 		String with the list of extracted POIs in XML
	 */
	public String extractPOIXMLsFromResponse(String body) {
		return extractPOIXMLsFromResponse(body, Integer.MAX_VALUE);
	}
	
	/**
	 * Extracts POIXMLs grouped in a CacheQueue from response body 
	 * It also centralizes the logging and exception handling abstracting it from source specific implementation
	 * @param body
	 * 		Response body string in JSON format
	 * @return
	 * 		CacheQueue object with the extracted POIs
	 */
	public CacheQueue<String> getCacheObjsFromResponse(String body){
		try {
			return extractPOIXMLCacheObjsFromResponse(body);
		} catch (JSONException e) {
			logger.severe(e.getMessage()+" At response body: "+body);
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * Extracts POIXMLs grouped in a CacheQueue from response body
	 * @param body
	 * 		Response body string in JSON format
	 * @return
	 * 		CacheQueue object with the extracted POIstion
	 */
	public abstract CacheQueue<String> extractPOIXMLCacheObjsFromResponse(String body) throws JSONException;
	
	/**
	 * Extracts POI ID from JSON object
	 * @param jsonObj
	 * 		JSONObject with the information to be extracted
	 * @return
	 * 		ID String
	 * @throws JSONException
	 * 		if id cannot be extracted
	 */
	public abstract String extractId(JSONObject jsonObj) throws JSONException;
	
	/**
	 * Extracts POI Timestamp from JSON object
	 * @param jsonObj
	 * 		JSONObject with the information to be extracted
	 * @return
	 * 		Timestamp (Unix Time in milliseconds)
	 * @throws JSONException
	 * 		if timestamp cannot be extracted
	 */
	public abstract long extractTimestamp(JSONObject jsonObj) throws JSONException;
	
	/**
	 * Extracts point coordinate from JSON object 
	 * @param jsonObj
	 * 		JSONObject with the information to be extracted
	 * @return
	 * 		Point coordinate indicating POI's location
	 */
	public abstract Point extractPoint(JSONObject jsonObj);
	
	/**
	 * Extracts Location object from JSON object returning null if no data could be found
	 * @param jsonObj
	 * 		JSONObject with the information to be extracted
	 * @return
	 * 		Location Object containing Point coordinate and Address if available
	 */
	public abstract Location extractLocation(JSONObject jsonObj);
	
	/**
	 * Extracts POI Label object from JSON object returning null if no data could be found
	 * @param jsonObj
	 * 		JSONObject with the information to be extracted
	 * @return
	 * 		POI Label string
	 */
	public abstract String extractLabel(JSONObject jsonObj);
	
	/**
	 * Extracts Social Event message object from JSON object returning null if no data could be found
	 * @param jsonObj
	 * 		JSONObject with the information to be extracted
	 * @return
	 * 		String with Social Event message
	 */
	public abstract String extractMessage(JSONObject jsonObj);
	
	/**
	 * Extracts POI description from JSON object
	 * @param jsonObj
	 * 		JSONObject with the information to be extracted
	 * @return
	 * 		String with POI description
	 */
	public abstract String extractDescription(JSONObject jsonObj);
	
	/**
	 * Extracts User author of Social Event from JSON object
	 * @param jsonObj
	 * 		JSONObject with the information to be extracted
	 * @return
	 * 		User author of Social Event
	 */
	public abstract User extractFrom(JSONObject jsonObj);
	
	/**
	 * Extracts Users recipient of Social Event from JSON Object
	 * @param jsonObj
	 * 		JSONObject with the information to be extracted 
	 * @return
	 * 		List of Users recipient of Social Event 
	 */
	public abstract ArrayList<User> extractToList(JSONObject jsonObj);
	
	/**
	 * Extracts Users tagged in Social Event from JSON Object
	 * @param jsonObj
	 * 		JSONObject with the information to be extracted 
	 * @return
	 * 		List of Users tagged in Social Event 
	 */
	public abstract ArrayList<TaggedUser> extractTagList(JSONObject jsonObj);
	
	/**
	 * Extracts CSV String with Social Event keywords from JSON Object
	 * @param jsonObj
	 * 		JSONObject with the information to be extracted
	 * @return
	 * 		CSV String with Social Event keywords
	 */
	public abstract String extractKeywords(JSONObject jsonObj);
	
	/**
	 * Extracts Social Event URL from JSON Object
	 * @param jsonObj
	 * 		JSONObject with the information to be extracted
	 * @return
	 * 		String with Social Event URL
	 */
	public abstract String extractUrl(JSONObject jsonObj);
	
	/**
	 * Extracts Social Event Link object from JSON Object
	 * @param jsonObj
	 * 		JSONObject with the information to be extracted
	 * @return
	 * 		Social Event link
	 */
	public abstract Link extractLink(JSONObject jsonObj);
	
	/**
	 * Extracts source name from JSON Object
	 * @param jsonObj
	 * 		JSONObject with the information to be extracted
	 * @return
	 * 		Source name
	 */
	public abstract String extractSource(JSONObject jsonObj); 
	
	/**
	 * Extracts media object from JSON Object if available
	 * @param jsonObj
	 * @return
	 * 		Media object (video or photo) including link to content and type plus extra information if available
	 */
	public abstract Media extractMediaContent(JSONObject jsonObj);
	
	/**
	 * Extracts media object thumbnail from JSON Object if available
	 * @param jsonObj
	 * @return
	 * 		Media object () including link to content and type plus extra information if available
	 */
	public abstract Media extractMediaThumbnail(JSONObject jsonObj);
	
	@Deprecated
	public abstract String extractAction(JSONObject jsonObj);
}
