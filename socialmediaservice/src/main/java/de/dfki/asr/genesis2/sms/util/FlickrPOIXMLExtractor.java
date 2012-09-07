package de.dfki.asr.genesis2.sms.util;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.dfki.asr.genesis2.sms.cache.CacheObject;
import de.dfki.asr.genesis2.sms.cache.CacheQueue;

/**
 * FlickrPOIXMLExtractor Class
 * 
 * This extension of SocialPOIXMLExtractor implements the abstract methods defined.
 * 
 * @author Andre de Oliveira Melo
 *
 */
public class FlickrPOIXMLExtractor extends SocialPOIXMLExtractor {
	private static final String source = "flickr";
	private static final String FLICKR_PHOTOS = "http://www.flickr.com/photos/";
	private static final String FLICKR_PEOPLE = "http://www.flickr.com/people/";
	
	@Override
	public CacheQueue<String> extractPOIXMLCacheObjsFromResponse(String body) throws JSONException {
		CacheQueue<String> list = new CacheQueue<String>();
		JSONObject jobj = new JSONObject(body);
		JSONArray results = jobj.getJSONObject("photos").getJSONArray("photo");
		for (int i=0; i<results.length(); i++) {
			JSONObject o = results.getJSONObject(i);	
			String id = extractId(o);
			long timestamp = extractTimestamp(o);
			String xml= this.extractPOIXML(o) + "\n";		
			list.add(new CacheObject<String>(id, timestamp, xml));
		}
		
		return list;
	}

	
	@Override
	public String extractId(JSONObject jsonObj) throws JSONException {
		return jsonObj.getString("id");
	}


	@Override
	public Point extractPoint(JSONObject jsonObj) {
		try {
			double lat = jsonObj.getDouble("latitude");
			double lon = jsonObj.getDouble("longitude");
			return new Point(lat, lon);
		} catch (JSONException e) {
			return null;
		}	

	}
	
	@Override
	public Location extractLocation(JSONObject jsonObj) {
		Point point = extractPoint(jsonObj);
		Address address = null;	// Flicker has no address data
		if (point==null) {
			return null;
		}
		return new Location(point, address);
	}

	@Override
	public String extractLabel(JSONObject jsonObj) {
		try {
			String title = jsonObj.getString("title");
			if (!title.equals(""))
				return title;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String extractMessage(JSONObject jsonObj) {
		return null;
	}

	@Override
	public String extractDescription(JSONObject jsonObj) {
		try {
			String desc = jsonObj.getJSONObject("description").getString("_content");	
			if (!desc.equals("")) 
				return desc;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public User extractFrom(JSONObject jsonObj) {
		try {
			String username = jsonObj.getString("ownername");
			String id = jsonObj.getString("owner");
			String href = FLICKR_PEOPLE + id;
			String name = null;
			Link link = new Link(href, "text/html");
			String url = "http://farm" + jsonObj.getInt("iconfarm") + ".staticflickr.com/" + jsonObj.getString("iconserver") + "/buddyicons/" + id + ".jpg";
			Media image = new Media(url, "image/jpeg");
			User user = new User(id, username, name, link, image);
			return user;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		
		
	}

	@Override
	public ArrayList<User> extractToList(JSONObject jsonObj) {
		return null;
	}

	@Override
	public ArrayList<TaggedUser> extractTagList(JSONObject jsonObj) {
		return null;
	}

	@Override
	public String extractKeywords(JSONObject jsonObj) {
		try {
			String hashTags = jsonObj.getString("tags").replace(" ", ",");
			if (!hashTags.equals("")) 
				return hashTags;
		} catch (JSONException e) {}
		return null;
	}

	@Override
	public String extractUrl(JSONObject jsonObj) {
		try {
			String userId = jsonObj.getString("owner");
			String photoId = jsonObj.getString("id");
			return FLICKR_PHOTOS + userId + "/" + photoId;
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
		return jsonObj.getLong("dateupload")*1000;
	}

	@Override
	public String extractSource(JSONObject jsonObj) {
		return source;
	}

	@Override
	public Media extractMediaContent(JSONObject jsonObj) {
		try {			
			char ending = jsonObj.has("url_o")? 'o': 'z';			
			String url = jsonObj.getString("url_" + ending);
			String type = "image/jpeg";
			Media media = new Media(url, type);
			media.setUrl(jsonObj.getString("url_" + ending));
			media.setHeight(jsonObj.getInt("height_" + ending));
			return media;
		} catch (JSONException e) {
			return null;
		}
	}


	@Override
	public Media extractMediaThumbnail(JSONObject jsonObj) {
		try {
			String url = jsonObj.getString("url_t");
			String type = "image/jpeg";
			Media media = new Media(url, type);
			media.setHeight(jsonObj.getInt("height_t"));
			media.setWidth(jsonObj.getInt("width_t"));
			return media;
		} catch (JSONException e) {
			return null;
		}
	}


	@Override
	public Link extractLink(JSONObject jsonObj) {
		try {
			String url = "http://www.flickr.com/photos/";
			String userId = jsonObj.getString("owner");
			String photoId = jsonObj.getString("id");
			url += userId + "/" + photoId;
			return new Link(url , "text/html");
		} catch (JSONException e) {
			return null;
		}
		
	}

}
