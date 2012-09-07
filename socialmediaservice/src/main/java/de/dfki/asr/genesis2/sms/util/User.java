package de.dfki.asr.genesis2.sms.util;

/**
 * User Class
 * 
 * This class contains information about Social Network User
 * 
 * @author Andre de Oliveira Melo
 *
 */
public class User {
	private String id = null;
	private String username = null;
	private String name = null;
	private Link link = null;
	private Media image = null;
	
	/**
	 * User blank constructor
	 */
	public User() {
		super();
	}
	
	/**
	 * User Class constructor
	 * @param id
	 * 		User ID
	 * @param username
	 * 		Username
	 * @param name
	 * 		User Name
	 * @param link
	 * 		Link to User's profile page
	 * @param image
	 * 		User's profile picture
	 */
	public User(String id, String username, String name, Link link, Media image) {
		this.id = id;
		this.name = name;
		this.username = username;	
		this.link = link;
		this.image = image;
	}	

	
	public String getUsername() {
		return this.username;
	}
	
	public String getName() {
		return this.name;
	}
	
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Link getLink() {
		return link;
	}

	public void setLink(Link link) {
		this.link = link;
	}

	public Media getImage() {
		return image;
	}

	public void setImage(Media image) {
		this.image = image;
	}
}
