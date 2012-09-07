package de.dfki.asr.genesis2.sms.util;

import java.util.ArrayList;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit; 
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * SocialEvent class
 * 
 * This class contains Social Network specific information added to POIXML to create SocialPOIXML
 * 
 * @author Andre de Oliveira Melo
 *
 */
public class SocialEvent {
	
	private String source = null;
	private String id = null;
	private Long timestamp = null;
	private Integer priority = null;	
	private String title = null;
	private String message = null;
	private String description = null;
	private String keywords = null;
	private String url = null;
	private User from = null;
	
	@XStreamAsAttribute
	private String lang = null;
	
	@XStreamAlias("media-content")
	private Media mediaContent = null;
	
	@XStreamAlias("media-thumbnail")
	private Media mediaThumbnail = null;
	
	@XStreamImplicit(itemFieldName="to")
	private ArrayList<User> toList = null;
	
	@XStreamImplicit(itemFieldName="tag")
	private ArrayList<TaggedUser> tagList = null;


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getTimestamp() {
		return timestamp.longValue();
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = new Long(timestamp);
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public User getFrom() {
		return from;
	}

	public void setFrom(User from) {
		this.from = from;
	}

	public ArrayList<User> getToList() {
		return toList;
	}

	public void setToList(ArrayList<User> toList) {
		this.toList = toList;
	}
	
	public void setToList(User user) {
		ArrayList<User> toList = new ArrayList<User>();
		toList.add(user);
		this.toList = toList;
	}

	public ArrayList<TaggedUser> getTagList() {
		return tagList;
	}

	public void setTagList(ArrayList<TaggedUser> tagList) {
		this.tagList = tagList;
	}
	
	public void setTagList(TaggedUser user) {
		ArrayList<TaggedUser> tagList = new ArrayList<TaggedUser>();
		toList.add(user);
		this.tagList = tagList;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public Media getMediaContent() {
		return mediaContent;
	}

	public void setMediaContent(Media mediaContent) {
		this.mediaContent = mediaContent;
	}

	public Media getMediaThumbnail() {
		return mediaThumbnail;
	}

	public void setMediaThumbnail(Media mediaThumbnail) {
		this.mediaThumbnail = mediaThumbnail;
	}

	public String getLanguage() {
		return lang;
	}

	public void setLanguage(String lang) {
		this.lang = lang;
	}
}
