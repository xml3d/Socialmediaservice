package de.dfki.asr.genesis2.sms.util;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * TaggedUser Class
 * 
 * Extension of User including text tag information (offset and length)
 * @see User
 * 
 * @author ande01
 *
 */
public class TaggedUser extends User {

	@XStreamAsAttribute
	private Integer offset = null;
	
	@XStreamAsAttribute
	private Integer length = null;

	/**
	 * TaggedUser blank contructor;
	 */
	public TaggedUser() {
		super();
	}
	
	/**
	 * Tagged User Constructor
	 * @param id
	 * 		User ID
	 * @param username
	 * 		Username
	 * @param name
	 * 		User's name
	 * @param link
	 * 		Link to User's profile page
	 * @param image
	 * 		User's profile picture
	 * @param offset
	 * 		Position in message where tag starts
	 * @param length
	 * 		Length of tag in message
	 */
	public TaggedUser(String id, String username, String name, Link link, Media image, int offset, int length) {
		super(id,username,name,link,image);
		this.length = new Integer(length);
		this.offset = new Integer(offset);
	}

	public int getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
}
