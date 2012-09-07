package de.dfki.asr.genesis2.sms.util;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Media Class
 * 
 * This class contains information about media resources (picture, video, audio, etc.)
 * 
 * @author ande01
 *
 */
public class Media {
	@XStreamAsAttribute
	private String src = null;
	
	@XStreamAsAttribute
	private String type = null;
	
	@XStreamAsAttribute
	private Integer height = null;
	
	@XStreamAsAttribute
	private Integer width = null;
	
	@XStreamAsAttribute
	private Integer duration = null; // Duration in seconds, applies only for video/audio
	
	/**
	 * Media blank class constructor
	 */
	public Media() {
	}
	
	/**
	 * Media Class contructor
	 * @param url
	 * 		URL of media resource
	 * @param type
	 * 		MIME type of media resource
	 */
	public Media(String url, String type){
		this.setUrl(url);
		this.setType(type);
	}

	public String getUrl() {
		return src;
	}

	public void setUrl(String url) {
		this.src = url;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}
}
