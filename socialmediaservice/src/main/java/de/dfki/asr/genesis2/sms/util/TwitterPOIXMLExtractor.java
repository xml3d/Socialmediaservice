package de.dfki.asr.genesis2.sms.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.dfki.asr.genesis2.sms.cache.CacheObject;
import de.dfki.asr.genesis2.sms.cache.CacheQueue;

/**
 * TwitterPOIXMLExtractor Class
 * 
 * This class extends SocialPOIXMLExtractor implementing the abstract methods defined.
 * It also features a 'isStream' variable to indicate whether the input data is coming
 * from Twitter's Streaming API or Search API. It's necessary because the APIs present
 * slight differences in the format.
 * 
 * 
 * @author Andre de Oliveira Melo
 *
 */
public class TwitterPOIXMLExtractor extends SocialPOIXMLExtractor {
	
	private static final String source = "twitter";
	private static final String twitterStreamDateFormat = "EEE MMM dd HH:mm:ss Z yyyy";  // Date format used in Stream API
	private static final String twitterSearchDateFormat = "EEE, dd MMM yyyy HH:mm:ss Z"; // Date format used in Search API
	private static final SimpleDateFormat twitterStreamformatter = new SimpleDateFormat(twitterStreamDateFormat, Locale.ENGLISH);
	private static final SimpleDateFormat twitterSearchformatter = new SimpleDateFormat(twitterSearchDateFormat, Locale.ENGLISH);
	private static final String twitterURL = "http://twitter.com/";
	private final boolean isStream;
	
	/**
	 * Class Constructor
	 * @param isStream
	 * 		Specifies whether JSON input comes from Twitter Stream API (true) or Search API (false). It's required because data is structured differently
	 */
	public TwitterPOIXMLExtractor(boolean isStream) {
		this.isStream = isStream;
	}
	
	@Override
	public CacheQueue<String> extractPOIXMLCacheObjsFromResponse(String body) throws JSONException {
		CacheQueue<String> list = new CacheQueue<String>();
		JSONObject jobj = null;
		JSONArray results = null;
		try {
			if (body.startsWith("{")) {
				jobj = new JSONObject(body);
				list.setMaxId(jobj.getString("max_id"));
				results = jobj.getJSONArray("results");
			}
			else if (body.startsWith("[")) {
				results = new JSONArray(body);
			}
		}
		catch (JSONException e) { // Response might be broken (Twitter responses have size limitation), so try to recover JSON
			int lastValidPos = body.lastIndexOf("},{\"created");
			if (lastValidPos>0) {
				String recoveredBody = body.substring(0, lastValidPos) + "}]";
				if (body.startsWith("{")) 
					recoveredBody += "}";
				jobj = new JSONObject(recoveredBody);
			}
			else 
				throw e;
		}
		
		for (int i=0; results!=null & i<results.length(); i++) {
			JSONObject o = results.getJSONObject(i);
			
			String id = extractId(o);
			long timestamp = extractTimestamp(o);
			String xml= this.extractPOIXML(o) + "\n";
			
			list.add(new CacheObject<String>(id, timestamp, xml));
		}
		
		return list;
	}

	@Override
	public Point extractPoint(JSONObject jsonObj) {
		try {
			JSONArray ja = jsonObj.getJSONObject("coordinates").getJSONArray("coordinates");
			double latitude = ja.getDouble(1);
			double longitude = ja.getDouble(0);
			return new Point(latitude,longitude);
		} catch (JSONException e) {}
		
		try {
			JSONArray ja = jsonObj.getJSONObject("geo").getJSONArray("coordinates");
			double latitude = ja.getDouble(0);
			double longitude = ja.getDouble(1);
			return new Point(latitude,longitude);
		} catch (JSONException e) {}
		
		try {
			JSONArray ja = jsonObj.getJSONObject("place").getJSONObject("bounding_box").getJSONArray("coordinates").getJSONArray(0);
			double latitude = 0;
			double longitude = 0;
			int len = ja.length(); 
			for (int i=0; i<len; i++) {
				JSONArray jsonVertex = ja.getJSONArray(i);
				latitude += jsonVertex.getDouble(1);
				longitude += jsonVertex.getDouble(0);
			}
			// TODO Calculate precision and add to 
			latitude /= len;
			longitude /= len;	
			return new Point(latitude,longitude);
		} catch (JSONException e) {}
		
		return null;
	}

	private Address extractAddress(JSONObject jsonObj) {
		try {
			jsonObj = jsonObj.getJSONObject("place");
			String country = jsonObj.getString("country_code");
			String place_type = jsonObj.getString("place_type");
			String region = null;
			if (place_type.equalsIgnoreCase("city")) 
				region = jsonObj.getString("name");
			Address address = new Address(country, null, region);
			return address;
		} catch (JSONException e) {
			return null;
		}
	}

	@Override
	public Location extractLocation(JSONObject jsonObj) {
		Point point = extractPoint(jsonObj);
		Address address = extractAddress(jsonObj);
		if (point==null && address==null) {
			return null;
		}
		return new Location(point, address);
	}

	@Override
	public String extractId (JSONObject jsonObj) throws JSONException {
		return jsonObj.getString("id_str");
	}

	@Override
	public String extractLabel(JSONObject jsonObj) {
		return null;
	}

	@Override
	public String extractMessage(JSONObject jsonObj) {
		try {
			return jsonObj.getString("text");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String extractDescription(JSONObject jsonObj) {
		return null;
	}

	@Override
	public User extractFrom(JSONObject jsonObj) {
		//Try Twitter Stream Layout
		if (isStream) {
			try {
				jsonObj = jsonObj.getJSONObject("user");
				String username = jsonObj.getString("screen_name");
				String name = jsonObj.getString("name");	
				String id = jsonObj.getString("id_str");
				String url = twitterURL + username;
				String profImageUrl = jsonObj.getString("profile_image_url");
				Link link = new Link(url,"text/html");
				Media image = new Media(profImageUrl, "image/jpeg");
				User user = new User(id, username, name, link, image);
				return user;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
		// Try Twitter Search Layout
			try {
				String username = jsonObj.getString("from_user");
				String name = jsonObj.getString("from_user_name");
				String id = jsonObj.getString("from_user_id_str");			
				String url = twitterURL + username;
				String profImageUrl = jsonObj.getString("profile_image_url");
				Link link = new Link(url,"text/html");
				Media image = new Media(profImageUrl, "image/jpeg");
				User user = new User(id, username, name, link, image);
				return user;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return null;
	}

	@Override
	public String extractKeywords (JSONObject jsonObj) {
		try {
			String hashTagsCSV = "";
			jsonObj = jsonObj.getJSONObject("entities");
			JSONArray hashTags = jsonObj.getJSONArray("hashtags");
					
			for (int i=0; i < hashTags.length(); i++) {
				hashTagsCSV += hashTags.getJSONObject(i).getString("text") + ",";
			}
			// Eliminates last comma on the CSV string
			if (hashTagsCSV.endsWith(",")) 
				hashTagsCSV = hashTagsCSV.substring(0, hashTagsCSV.length()-1);
			
			if (hashTagsCSV.length()>0)
				return hashTagsCSV;
		} catch (JSONException e) {
		}
		return null;
	}

	@Override
	public String extractUrl (JSONObject jsonObj) {
		try {
			return extractFrom(jsonObj).getLink().getHref() + "/status/" + extractId(jsonObj);
		} catch (JSONException e) {
			return null;
		}
	}

	@Override
	public String extractAction(JSONObject jsonObj) {
		return null;
	}

	@Override
	public long extractTimestamp(JSONObject jsonObj) throws JSONException {
		//Try Twitter Stream Layout
		String dateString = jsonObj.getString("created_at");
		try {
			if (isStream) {
				Date date = (Date)twitterStreamformatter.parse(dateString);
				return date.getTime();
			}
			//Try Twitter Search Layout
			else {
				Date date = (Date)twitterSearchformatter.parse(dateString);
				return date.getTime();
			}
		} catch (ParseException e) {
			throw new JSONException(e);
		}
	}

	@Override
	public String extractSource(JSONObject jsonObj) {
		return TwitterPOIXMLExtractor.source;
	}

	@Override
	public ArrayList<User> extractToList(JSONObject jsonObj) {	
		try {
			String username = jsonObj.getString("in_reply_to_screen_name");
			String id = jsonObj.getString("in_reply_to_user_id_str");
			if (username=="null" || id=="null")
				return null;
			String href = twitterURL + username;
			Link link = new Link(href, "text/html");
			Media image = null;
			String name = null;
			User to = new User(id, username, name, link, image);
			ArrayList<User> toList = new ArrayList<User>();
			toList.add(to);
			return toList;
		} catch (JSONException e) {
			return null;
		}
	}

	@Override
	public ArrayList<TaggedUser> extractTagList(JSONObject jsonObj) {
		try {
			JSONArray tagArray = jsonObj.getJSONObject("entities").getJSONArray("user_mentions");			
			ArrayList<TaggedUser> tagList = new ArrayList<TaggedUser>();
			
			int len = tagArray.length();
			for (int i=0; i<len; i++) {
				jsonObj = tagArray.getJSONObject(i);
				String username = jsonObj.getString("screen_name");
				String name = jsonObj.getString("name");
				String id = jsonObj.getString("id_str");
				Link link = new Link(twitterURL + username, "text/html");
				JSONArray jsonIndices = jsonObj.getJSONArray("indices");
				int offset = jsonIndices.getInt(0);
				int length = jsonIndices.getInt(1);
				Media image = null;
				tagList.add(new TaggedUser(id, username, name, link, image, offset, length));
			}
			return len==0? null: tagList;
		} catch (JSONException e) {
			return null;
		}
		
		
	}

	@Override
	public Media extractMediaContent(JSONObject jsonObj) {
		try {
			jsonObj = jsonObj.getJSONObject("entities").getJSONArray("media").getJSONObject(0);
			String url = jsonObj.getString("media_url"); // + ":large" ?
			String type = jsonObj.getString("type");
			Media media = new Media(url, type);
			return media;			
		} catch (JSONException e) {
			return null;
		}
	}

	@Override
	public Media extractMediaThumbnail(JSONObject jsonObj) {
		try {
			jsonObj = jsonObj.getJSONObject("entities").getJSONArray("media").getJSONObject(0);
			String type = jsonObj.getString("type");
			String url = jsonObj.getString("media_url") + ":thumb";
			Media media = new Media(url, type);
			return media;			
		} catch (JSONException e) {
			return null;
		}
	}

	@Override
	public Link extractLink(JSONObject jsonObj) {
		String url;
		try {
			url = extractFrom(jsonObj).getLink().getHref() + "/status/" + extractId(jsonObj);
			return new Link(url, "text/html");
		} catch (JSONException e) {
			return null;
		}
	}
}
