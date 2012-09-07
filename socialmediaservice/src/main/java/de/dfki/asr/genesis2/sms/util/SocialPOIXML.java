package de.dfki.asr.genesis2.sms.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.io.xml.DomDriver;

import de.dfki.asr.genesis2.sms.util.POIXML;

/**
 * Social POIXML class
 * 
 * This extension of POIXML class includes extra social network data
 * @see POIXML
 * 
 * @author Andre de Oliveira Melo
 *
 */
@XStreamAlias("poi")
public class SocialPOIXML extends POIXML{
	
	@XStreamAlias("social")
	private SocialEvent socialEvent;
	
	/**
	 * Empty class constructor
	 */
	public SocialPOIXML() {
	}
	
	/**
	 * Class constructor
	 * @param location	
	 * 		@see Location
	 * @param socialEvent
	 * 		@see SocialEvent
	 */
	public SocialPOIXML(Location location, SocialEvent socialEvent) {
		super.setLocation(location);
		this.setSocialEvent(socialEvent);
	}

	public SocialEvent getSocialEvent() {
		return socialEvent;
	}

	public void setSocialEvent(SocialEvent socialEvent) {
		this.socialEvent = socialEvent;
	}
	
	/**
	 * Converts SocialPOIXML class to XML String based on the class structure
	 */
	@Override
	public String toString() {
		XStream xStream = new XStream(new DomDriver());
		xStream.processAnnotations(this.getClass());
		return xStream.toXML(this);
	}
	
	/**
	 * Parses a XML string loading information into a SocialPOIXML object
	 * @param xml
	 * @return
	 */
	public static SocialPOIXML parseXML(String xml) {
		XStream xstream = new XStream();
		xstream.processAnnotations(SocialPOIXML.class);
		return (SocialPOIXML) xstream.fromXML(xml);
	}
}
