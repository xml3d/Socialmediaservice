package de.dfki.asr.genesis2.sms.util;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Link class
 * 
 * This class contains information about linked document
 * 
 * @author Andre de Oliveira Melo
 *
 */
public class Link {

	@XStreamAsAttribute
	private String type = null;
	
	@XStreamAsAttribute
	private String href = null;

	@XStreamAsAttribute
	private String rel = null;
	
	
	/**
	 * Link class constructor
	 * @param href
	 * 		Address of the linked document
	 * @param type
	 * 		MIME type of the linked document
	 */
	public Link (String href, String type) {
		this.href = href;
		this.type = type;
	}
	
	public String getHref() {
		return href;
	}
	
	public void setHref(String href) {
		this.href = href;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}

	public String getRel() {
		return rel;
	}

	public void setRel(String rel) {
		this.rel = rel;
	}
}
