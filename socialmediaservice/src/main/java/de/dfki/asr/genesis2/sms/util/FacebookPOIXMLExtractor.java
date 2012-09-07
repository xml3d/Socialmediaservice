package de.dfki.asr.genesis2.sms.util;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.dfki.asr.genesis2.sms.cache.CacheObject;
import de.dfki.asr.genesis2.sms.cache.CacheQueue;

/**
 * FacebookPOIXMLExtractor Class
 * 
 * This extension of SocialPOIXMLExtractor implements the abstract methods defined.
 * It also features the 'ignoreShares' variable which gives the option of ignoring share posts.
 * As Facebook API usually returns multiple share posts of a same original object, resulting in a big
 * amount of redundant and irrelevant information. In this case, with ignoreShares=true, a single
 * POI is created with the original object post and the shares are ignored.
 * 
 * @author Andre de Oliveira Melo
 *
 */
public class FacebookPOIXMLExtractor extends SocialPOIXMLExtractor {

	private static final String source = "facebook";
	private static final String FACEBOOK_URL = "http://www.facebook.com/";
	private static final String GRAPH_API_URL = "https://graph.facebook.com/";
	private boolean ignoreShares = false;	// If true don't consider shares a post, only shared object
	private boolean postIsShare = false;	// Identifies if post is a share
	
	/**
	 * Class Constructor with default ignoreShares=true
	 * @see de.dfki.asr.genesis2.sms.util.FacebookPOIXMLExtractor#FacebookPOIXMLExtractor(boolean)
	 */
	public FacebookPOIXMLExtractor() {
		this(true);
	}
	
	/**
	 * Class Constructor specifying whether to ignore share posts
	 * @param ignoreShares
	 * 		If true, ignores share posts and consider only its shared objects (in order to avoid multiple share posts from same objects)
	 */
	public FacebookPOIXMLExtractor(boolean ignoreShares) {
		this.ignoreShares = ignoreShares;
	}
	
	@Override
	public CacheQueue<String> extractPOIXMLCacheObjsFromResponse(String body) throws JSONException {
		CacheQueue<String> list = new CacheQueue<String>();
		JSONObject jsonObj = new JSONObject(body);
		JSONArray results = jsonObj.getJSONArray("data");
		for (int i=0; i<results.length(); i++) {
			JSONObject o = results.getJSONObject(i);
			if (ignoreShares)
				postIsShare = checkIfPostIsShare(o);
			String id = extractId(o);
			long timestamp = extractTimestamp(o);
			String xml= this.extractPOIXML(o) + "\n";
			
			list.add(new CacheObject<String>(id, timestamp, xml));
		}		
		return list;
	}
	
	private boolean checkIfPostIsShare(JSONObject jsonObj) {
		return jsonObj.has("object_id");
	}
	
	@Override
	public String extractId(JSONObject jsonObj) {
		try {
			if (ignoreShares && postIsShare) 
				return jsonObj.getString("object_id");
			else {
				String id = jsonObj.getString("id");
				id = id.substring(id.indexOf('_')+1);  // ID is in the format UserID_PostID, PostID is already unique, so UserID can be ignored 
				return id;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Point extractPoint(JSONObject jsonObj) {
		try {
			jsonObj = jsonObj.getJSONObject("place").getJSONObject("location");
			double lat = jsonObj.getDouble("latitude");
			double lon = jsonObj.getDouble("longitude");
			return new Point(lat, lon);
		} catch (JSONException e) {
			return null;
		}
	}
	
	
	public Address extractAddress(JSONObject jsonObj) {
		try {
			jsonObj = jsonObj.getJSONObject("place").getJSONObject("location");

			Address address = new Address();
			if (jsonObj.has("street"))
				address.setStreet(jsonObj.getString("street"));
			if (jsonObj.has("city"))
				address.setRegion(jsonObj.getString("city"));
			if (jsonObj.has("country"))
				address.setCountry(jsonObj.getString("country"));
			if (jsonObj.has("zip"))
				address.setZip(jsonObj.getString("zip"));
			
			if (!address.isEmpty()) 
				return address;
		} catch (JSONException e) {}
		
		return null;
	}

	@Override
	public Location extractLocation(JSONObject jsonObj) {
		try {
			Point point = extractPoint(jsonObj);
			Address address = extractAddress(jsonObj);
			
			if (point!= null || address!= null)  {
				Location location = new Location(point, address);
				jsonObj = jsonObj.getJSONObject("place");
				if (jsonObj.has("name"))
					location.setName(jsonObj.getString("name"));	
				return location;
			}
		} catch (JSONException e) {}
		return null;
		
	}

	@Override
	public String extractLabel(JSONObject jsonObj) {
		try {
			if (ignoreShares && postIsShare)
				return jsonObj.getString("name");
			else
				return jsonObj.getString("story");
		} catch (JSONException e) {
			return null;
		}	
	}

	@Override
	public String extractMessage(JSONObject jsonObj) {
		try {
			return jsonObj.getString("message");
		} catch (JSONException e) {
			return null;
		}
	}

	@Override
	public String extractDescription(JSONObject jsonObj) {
		try {
			return jsonObj.getString("caption");
		} catch (JSONException e) {
			return null;
		}	
	}

	@Override
	public User extractFrom(JSONObject jsonObj) {
		try {
			if (ignoreShares && postIsShare) {
				String id = jsonObj.getString("link");
				if (id.contains("?fbid="))
					id = id.substring(id.lastIndexOf('.')+1,id.lastIndexOf('&'));
				if (id.contains("?v="))
					id = id.substring(id.lastIndexOf('=')+1);
				
				jsonObj = jsonObj.getJSONArray("properties").getJSONObject(0);			
				Media image = new Media(GRAPH_API_URL + id + "/picture", "image/jpeg");
				String name = jsonObj.getString("text");
				String href = null;
				if (jsonObj.has("href")) href = jsonObj.getString("href");
				Link link = new Link(href, "text/html");
				return new User(id, null, name, link, image);
			}
			else {
				jsonObj = jsonObj.getJSONObject("from");
				String id  = jsonObj.getString("id");
				String name = jsonObj.getString("name");
				Link link = new Link(FACEBOOK_URL + id, "text/html");
				Media image = new Media(GRAPH_API_URL + id + "/picture", "image/jpeg");
				return new User(id, null, name, link, image);
			}
		} catch (JSONException e) {
			return null;
		}
		
	}

	@Override
	public ArrayList<User> extractToList(JSONObject jsonObj) {
		ArrayList<User> tagList = new ArrayList<User>();
		try {
			JSONArray jsonTagArray = jsonObj.getJSONObject("to").getJSONArray("data");
			for (int i=0; i<jsonTagArray.length(); i++) {
				jsonObj = jsonTagArray.getJSONObject(i);
				String id = jsonObj.getString("id");
				String name = jsonObj.getString("name");
				Link link = new Link(FACEBOOK_URL + id, "text/html");
				Media image = new Media(GRAPH_API_URL + id + "/picture", "image/jpeg");
				tagList.add(new User(id, null, name, link, image));
			}
		} catch (JSONException e) {}
		
		return (tagList.size() > 0) ? tagList : null;
	}

	@Override
	public ArrayList<TaggedUser> extractTagList(JSONObject jsonObj) {
		ArrayList<TaggedUser> tagList = new ArrayList<TaggedUser>();
		// Story_Tags are normally irrelevant, always like "UserX shared a photo" with offset 0, and it happens that tagged user is always same as author
		/*try {
			if (jsonObj.has("story_tags") && (!ignoreShares || !postIsShare)) {
				JSONObject jsonStoryTags = jsonObj.getJSONObject("story_tags");
				String names[] = JSONObject.getNames(jsonStoryTags);		
				for (int i=0; i<names.length; i++) {
					JSONObject jsonTemp = jsonStoryTags.getJSONArray(names[i]).getJSONObject(0);
					String id = jsonTemp.getString("id");
					String name = jsonTemp.getString("name");
					Link link = new Link(FACEBOOK_URL + id, "text/html");
					Media image = new Media(GRAPH_API_URL + id + "/picture", "image/jpeg");
					int offset = jsonTemp.getInt("offset");
					int length = jsonTemp.getInt("length");
					tagList.add(new TaggedUser(id, name, link, image, offset, length));
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}*/
		try {
			if (jsonObj.has("message_tags")) {
				JSONObject jsonStoryTags = jsonObj.getJSONObject("message_tags");
				String names[] = JSONObject.getNames(jsonStoryTags);		
				for (int i=0; i<names.length; i++) {
					JSONObject jsonTemp = jsonStoryTags.getJSONArray(names[i]).getJSONObject(0);
					String id = jsonTemp.getString("id");
					String name = jsonTemp.getString("name");
					Link link = new Link(FACEBOOK_URL + id, "text/html");
					Media image = new Media(GRAPH_API_URL + id + "/picture", "image/jpeg");
					int offset = jsonTemp.getInt("offset");
					int length = jsonTemp.getInt("length");
					tagList.add(new TaggedUser(id, null, name, link, image, offset, length));
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return (tagList.size() > 0) ? tagList : null;
	}

	@Override
	public String extractKeywords(JSONObject jsonObj) {
		return null;
	}

	@Override
	public String extractUrl(JSONObject jsonObj) {
		return null;
	}

	@Override
	public Link extractLink(JSONObject jsonObj) {
		try {
			if (ignoreShares && postIsShare) {
				return new Link(jsonObj.getString("link"), "text/html");
			}
			else {
				String id = jsonObj.getString("id");
				if (id != null) {
					String user_post[] = id.split("_");
					String href = FACEBOOK_URL + user_post[0] + "/posts/" + user_post[1];
					return new Link(href , "text/html");
				}
			}
		}
		catch (IndexOutOfBoundsException e){}
		catch (JSONException e) {} 
		return null;
	}

	@Override
	public String extractAction(JSONObject jsonObj) {
		return null;
	}

	@Override
	public long extractTimestamp(JSONObject jsonObj) {
		try {
			return jsonObj.getLong("created_time")*1000;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}		
	}

	@SuppressWarnings("static-access")
	@Override
	public String extractSource(JSONObject jsonObj) {
		return this.source;
	}

	@Override
	public Media extractMediaContent(JSONObject jsonObj) {
		try {
			String type =  jsonObj.getString("type");
			if (type.equals("video")) 
				return new Media(jsonObj.getString("source"), "application/x-shockwave-flash");
			if (type.equals("link")) 
				return new Media(jsonObj.getString("link"), "text/html");			
			if (type.equals("photo")) {
				String url = jsonObj.getString("picture");
				url = url.replace("_s.jpg", "_n.jpg");
				return new Media(url, "image/jpeg");				
			}

		} catch (JSONException e) {}
		return null;
	}

	@Override
	public Media extractMediaThumbnail(JSONObject jsonObj) {
		try {
			String url = jsonObj.getString("picture");
			return new Media(url, "image/jpeg");
		} catch (JSONException e) {
			return null;
		}		
	}


}
