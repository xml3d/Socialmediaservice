package de.dfki.asr.genesis2.sms.util;

import java.util.ArrayList;
import com.thoughtworks.xstream.annotations.XStreamAlias; 
import com.thoughtworks.xstream.annotations.XStreamImplicit; 

/**
 * SocialPOIXMLList class
 * 
 * This class contains a list of SocialPOIXML objects and 
 * 
 * @author Andre de Oliveir Melo
 *
 */
@XStreamAlias("pois")
public class SocialPOIXMLList {
	
	@XStreamImplicit(itemFieldName="poi")
	private ArrayList<SocialPOIXML> entries;

	public ArrayList<SocialPOIXML> getEntries() {
		return entries;
	}

	public void setEntries(ArrayList<SocialPOIXML> entries) {
		this.entries = entries;
	}
}
