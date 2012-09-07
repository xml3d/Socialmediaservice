package de.dfki.asr.genesis2.sms.util;


/**
 * Location Class
 * 
 * This class contains location information, and is structured according to W3C POI
 * 
 * @author Andre de Oliveira Melo
 *
 */
public class Location {

	private String name = null;
	private Address address = null;
	private Point point = null;
	//private BoundingBox boundingBox = null;
	
	/**
	 * Location class constructor
	 * @param latitude
	 * 		Point latitude
	 * @param longitude
	 * 		Point longitude
	 */
	public Location(double latitude, double longitude) {
		this.point = new Point(latitude,longitude);
	}
	
	/**
	 * Location class constructor
	 * @param point
	 * @param address
	 */
	public Location(Point point, Address address) {
		this.point = point;
		this.address = address;
	}
	
	/**
	 * Location blank constructor
	 */
	public Location() {
	}

	public Point getPoint() {
		return point;
	}

	public void setPoint(Point point) {
		this.point = point;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
