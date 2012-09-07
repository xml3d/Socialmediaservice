package de.dfki.asr.genesis2.sms.util;

import java.util.Collection;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Point location Class
 * 
 * @author Andre de Oliveira Melo
 *
 */
public class Point {
	
	// Coordinate Latitude in degrees
	@XStreamAsAttribute
	private Double latitude = null;
	
	// Coordinate Longitude in degrees
	@XStreamAsAttribute
	private Double longitude = null;
	

	//Altitude of point
	private Double altitude = null;
	
	/**
	 * Point class constructor
	 * @param latitude
	 * 		Latitude from point coordinate, in degrees
	 * @param longitude
	 * 		Longitude from point coordinate, in degrees
	 */
	public Point (double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	/**
	 * Calculates the distance between two points (ignores altitude) in kilometers
	 * @param p1
	 * 		Point 1
	 * @param p2
	 * 		Point 2
	 * @return
	 * 		Distance in kilometers
	 */
	public static double distance(Point p1, Point p2) {
		double theta = p1.getLongitude() - p2.getLongitude();
		double dist = Math.sin(Math.toRadians(p1.getLatitude())) * Math.sin(Math.toRadians(p2.getLatitude())) +
					+ Math.cos(Math.toRadians(p1.getLatitude())) * Math.cos(Math.toRadians(p2.getLatitude())) * Math.cos(Math.toRadians(theta));
		dist = Math.acos(dist);
		dist = Math.toDegrees(dist);
		dist = dist * 60 * 1.1515;
		dist = dist * 1.609344;
		return dist;
	}
	
	/**
	 * Creates a Point object given a coordinate string verifying if it'valid
	 * @param location
	 * 		Location coordinate string in the format latitude,longitude
	 * @return
	 * 		Point object 
	 * @throws IllegalArgumentException
	 * 		If location string is not valid
	 */
	public static Point parsePoint(String location) throws IllegalArgumentException{
		try {
			String latlong[] = location.split(",");
			if (latlong.length > 2)
				throw new IllegalArgumentException("Invalid argument: location="+location+", too many ',' in location string. It should be composed by of the form [-90,90],[-180,180]. Example: 37.4230,-122.0842" + "\n");
			String latString = latlong[0];
			String lonString = latlong[1];
			double lat = Double.parseDouble(latString);
			double lon = Double.parseDouble(lonString);
			if (lon>=-180 && lon<=180 && lat>=-90 && lat<=90)
				return new Point(lat,lon);
			else
				throw new IllegalArgumentException("Invalid argument: location="+location+", latitude or longitude out of bounds. It should be composed by of the form [-90,90],[-180,180]. Example: 37.4230,-122.0842" + "\n");
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("Invalid argument: location="+location+", it should be composed by of the form [-90,90],[-180,180]. Example: 37.4230,-122.0842" + "\n" + e.getMessage());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid argument: location="+location+", it should be composed by of the form [-90,90],[-180,180]. Example: 37.4230,-122.0842" + "\n" + e.getMessage());
		}
	}
	
	/**
	 * Checks if location string is valid
	 * @param location
	 * 		Location coordinate string in the format latitude,longitude
	 * @return
	 * 		true, if location is valid
	 * 		false otherwise
	 */
	public static boolean validLocation(String location) {
		try {
			String latlong[] = location.split(",");
			if (latlong.length != 2)
				return false;
			String latString = latlong[0];
			String lonString = latlong[1];
			double lat = Double.parseDouble(latString);
			double lon = Double.parseDouble(lonString);
			if (lon>=-180 && lon<=180 && lat>=-90 && lat<=90)
				return true;
		}
		catch (ArrayIndexOutOfBoundsException e){}
		catch (NumberFormatException e) {} 
		
		return false;
		
	}
	
	/**
	 * Truncates coordinates to a given number of decimal places
	 * @param numOfDecimalDigits
	 * 		Number of decimal digits
	 */
	public void truncate(int numOfDecimalDigits) {
		double n = Math.pow(10, numOfDecimalDigits);
		this.latitude = ((double) Math.round(this.latitude*n))/n;
		this.longitude = ((double) Math.round(this.longitude*n))/n;
	}
	
	/**
	 * Checks if coordinates are valid
	 * @return
	 * 		true, if coordinate is valid
	 * 		false otherwise
	 */
	public boolean isValid() {
		if (latitude!=null && longitude!=null)
			if (longitude>=-180 && longitude<=180 && latitude>=-90 && latitude<=90)
				return true;
		return false;
	}
	
	/**
	 * Calculates distance in kilometers to another Point object
	 * @param p
	 * 		Point to which calculate distance
	 * @return
	 * 		Distance in kilometers
	 */
	public double distance(Point p) {
		return Point.distance(this, p);
	}
	
	/**
	 * Converts point coordinates to String in the format latitude,longitude (ignores altitude)
	 */
	@Override 
	public String toString() {
		return this.latitude + "," + this.longitude;
	}
	
	/**
	 * Finds containing Maptile fort he given the zoom level
	 * @param zoom
	 * 		Maptile zoom level
	 * @return
	 * 		Maptile (x,y,zoom) where point is contained
	 */
    public MapTile getTileNumber(int zoom) {
 	   int xtile = (int)Math.floor( (this.longitude + 180) / 360 * (1<<zoom) ) ;
 	   int ytile = (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(this.latitude)) + 1 / Math.cos(Math.toRadians(this.latitude))) / Math.PI) / 2 * (1<<zoom) ) ;
 	   return new MapTile(xtile, ytile, zoom);
    }
    
    /**
     * Checks whether point object is contained in the given Maptile
     * @param tile
     * 		Maptile
     * @return
     * 		true, if point object is contained in Maptile
     * 		false otherwise
     */
    public boolean inMapTile(MapTile tile) {
    	return tile.equals(this.getTileNumber(tile.getZoom()));
    }
    
    
    /**
     * Checks whether point object is contained in one of the Maptiles in collection
     * @param tiles
     * 		Collection of Maptiles
     * @return
     * 		true, if point object is contained in one of Maptiles in collection
     * 		false otherwise
     */
    public boolean inMapTiles(Collection<MapTile> tiles) {
    	for (MapTile tile: tiles) {
    		if (this.inMapTile(tile)) {
    			return true;
    		}
    	}
    	return false;
    }
	
	public double getLatitude() {
		return this.latitude;
	}
	
	public double getLongitude() {
		return this.longitude;
	}
	
	void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	
	void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getAltitude() {
		return altitude;
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}	
}

