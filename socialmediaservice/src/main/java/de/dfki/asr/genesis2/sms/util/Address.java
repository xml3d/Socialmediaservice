package de.dfki.asr.genesis2.sms.util;

/**
 * Address class
 * 
 * This class contains information about an Address geo-reference, i.e. a civic address, such as a mailing address or a street address.
 * 
 * @author ande01
 *
 */
public class Address {
	private String country = null;
	private String language = null;
	private String region = null;
	private String street = null;
	private String zip = null;
	
	/**
	 * Address blank constructor
	 */
	public Address(){
	}
	
	/**
	 * Address class constructor
	 * @param country
	 * 		Country code	
	 * @param language
	 * 		Language code
	 * @param region
	 * 		Can contain a variable mix of administrative regions, neighborhood, city, state, etc.
	 */
	public Address(String country, String language, String region) {
		this.setCountry(country);
		this.setLanguage(language);
		this.setRegion(region);
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}
	
	/**
	 * Checks is Address object is empty (all attributes equal null)
	 * @return
	 * 		true, if all attributes equal null
	 * 		false otherwise
	 */
	public boolean isEmpty() {
		return (country==null && language==null && region==null && street==null && zip==null);
		
	}
	
	
}
