package de.dfki.asr.genesis2.sms.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.dfki.asr.genesis2.sms.cache.CacheObject;
import de.dfki.asr.genesis2.sms.cache.CacheQueue;


/**
 * YoutubePOIXMLExtractor Class
 * 
 * This extension of SocialPOIXMLExtractor implements the abstract methods defined.
 * 
 * @author Andre de Oliveira Melo
 *
 */
public class YoutubePOIXMLExtractor extends SocialPOIXMLExtractor{

	private static final String source = "youtube";
	private static final String youtubeDateFormat = "yyyy-mm-dd'T'HH:mm:ss.SSS'Z'";
	private static final SimpleDateFormat youtubeFormatter = new SimpleDateFormat(youtubeDateFormat, Locale.ENGLISH);

	@Override
	public CacheQueue<String> extractPOIXMLCacheObjsFromResponse(String body) throws JSONException {
		CacheQueue<String> list = new CacheQueue<String>();
		JSONObject jobj = new JSONObject(body);
		jobj = jobj.getJSONObject("feed");
		
		if (jobj.getJSONObject("openSearch$totalResults").getInt("$t") <= 0) // If response doens't contain any results
			return null;
		
		JSONArray results = jobj.getJSONArray("entry");
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
	public String extractId(JSONObject jsonObj) {
		try {
			String id = jsonObj.getJSONObject("id").getString("$t");
			return id.substring(id.lastIndexOf(':')+1);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public Point extractPoint(JSONObject jsonObj) {
		try {
			String  coord = jsonObj.getJSONObject("georss$where")
								   .getJSONObject("gml$Point")
								   .getJSONObject("gml$pos")
								   .getString("$t");
			String latlon[] = coord.split(" ");
			if (latlon.length >= 2) {
				double lat = Double.parseDouble(latlon[0]);
				double lon = Double.parseDouble(latlon[1]);
				return new Point(lat,lon);
			}
		} catch (JSONException e) { }
		
		return null;
	}

	@Override
	public Location extractLocation(JSONObject jsonObj) {
		Point point = extractPoint(jsonObj);
		Address address = extractAddress(jsonObj);
		if (point != null)
			return new Location(point, address);
		return null;
	}

	private Address extractAddress(JSONObject jsonObj) {
		return null;
	}


	@Override
	public String extractLabel(JSONObject jsonObj) {
		try {
			String msg = jsonObj.getJSONObject("title")
								.getString("$t");
			if (!msg.equals(""))
				return msg;
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
			String msg = jsonObj.getJSONObject("media$group")
								.getJSONObject("media$description")
								.getString("$t");
			if (!msg.equals(""))
				return msg;
		} catch (JSONException e) {}
		return null;
	}

	@Override
	public User extractFrom(JSONObject jsonObj) {
		try {
			jsonObj = jsonObj.getJSONArray("author").getJSONObject(0);
			String id = jsonObj.getJSONObject("yt$userId").getString("$t");
			String username = jsonObj.getJSONObject("name").getString("$t");
			String name = null;
			String url = "http://www.youtube.com/user/" + username;
			Link link = new Link(url,"text/html");
			Media image = null;
			User user = new User(id, username, name, link, image);
			return user;
		} catch (JSONException e) {
			System.out.println(jsonObj.toString());
			e.printStackTrace();
		}
		return null;
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
			String msg = jsonObj.getJSONObject("media$group")
								.getJSONObject("media$keywords")
								.getString("$t");
			if (!msg.equals(""))
				return msg;
		} catch (JSONException e) {}
		return null;
	}

	@Override
	public String extractUrl(JSONObject jsonObj) {
		try {
			jsonObj = jsonObj.getJSONArray("link").getJSONObject(0);
			return jsonObj.getString("href");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String extractAction(JSONObject jsonObj) {
		return null;
	}

	@Override
	public long extractTimestamp(JSONObject jsonObj) throws JSONException {
		try {
			jsonObj = jsonObj.getJSONObject("published");
			return youtubeFormatter.parse(jsonObj.getString("$t")).getTime();
		} catch (ParseException e) {}
		try {
			jsonObj = jsonObj.getJSONObject("updated");
			return youtubeFormatter.parse(jsonObj.getString("$t")).getTime();
		} catch (ParseException e) {
			throw new JSONException(e);
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
			jsonObj = jsonObj.getJSONObject("media$group").getJSONArray("media$content").getJSONObject(0);
			String url  = jsonObj.getString("url");
			String type = jsonObj.getString("type");
			Media media = new Media(url, type);
			media.setDuration(jsonObj.getInt("duration"));
			return media;
				
		} catch (JSONException e) {}
		return null;
	}

	@Override
	public Media extractMediaThumbnail(JSONObject jsonObj) {
		try {
			jsonObj = jsonObj.getJSONObject("media$group").getJSONArray("media$thumbnail").getJSONObject(0);
			String url = jsonObj.getString("url");	
			String type ="image/jpeg";
			Media media = new Media(url, type);
			media.setHeight(jsonObj.getInt("height"));
			media.setWidth(jsonObj.getInt("width"));
			return media;
				
		} catch (JSONException e) {}
		return null;
	}

	@Override
	public Link extractLink(JSONObject jsonObj) {
		try {
			String url = jsonObj.getJSONArray("link").getJSONObject(0).getString("href");
			return new Link(url, "text/html");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}


}
