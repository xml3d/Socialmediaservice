package de.dfki.asr.genesis2.sms.util;

import java.util.ArrayList;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * POIXML Class
 * 
 * This class contains POI information extracted from Sources' APIs structured according to a tailored version POI W3C
 * 
 * @author Andre de Oliveira Melo
 *
 */
public class POIXML {
	
	@XStreamAsAttribute
	private String id = null;

	private Location location = null;	
	private Long time = null;
	
	@XStreamImplicit(itemFieldName="category")
	private ArrayList<String> categoryList = null;
	
	private String label = null;
	private String description = null;
	private Link link = null;

	/**
	 * POIXML blank class constructor
	 */
	public POIXML() {
	}
	
	@Override
	public String toString() {
		XStream xStream = new XStream(new DomDriver());
		xStream.processAnnotations(this.getClass());
		return xStream.toXML(this);
	}

	public String getLabel() {
		return this.label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}

	public ArrayList<String> getCategoryList() {
		return categoryList;
	}

	public void setCategoryList(ArrayList<String> categoryList) {
		this.categoryList = categoryList;
	}
	
	public void setCategory(String category) {
		ArrayList<String> categorList = new ArrayList<String>();
		categorList.add(category);
		this.categoryList = categorList;
	}

	public long getTime() {
		return time.longValue();
	}

	public void setTime(long time) {
		this.time = new Long(time);
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}
	
	/**
	 * Parses a POI in XML format and loads information into a POIMXL object 
	 * @param xml
	 * 		POI in XML format
	 * @return
	 * 		POIXML object containing extracted information
	 */
	public static POIXML parseXML(String xml) {
		XStream xstream = new XStream();
		xstream.processAnnotations(POIXML.class);
		return (POIXML) xstream.fromXML(xml);
	}

	public Link getLink() {
		return link;
	}

	public void setLink(Link link) {
		this.link = link;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
