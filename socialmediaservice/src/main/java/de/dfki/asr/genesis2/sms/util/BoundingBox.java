package de.dfki.asr.genesis2.sms.util;

/**
 * BoundingBox class
 * 
 * This class contains a squared bounding box with the 4 boundaries (North and South latitudes and West and East longitudes)
 * 
 * @author Andre de Oliveira Melo
 *
 */
public class BoundingBox {
	private double north;
	private double south;
	private double east;
	private double west;
    
	/**
	 * Class constructor, converts a Maptile to a BoundingBox
	 * @param x
	 * 		Maptile X coordinate
	 * @param y
	 * 		Maptile Y coordinate 
	 * @param zoom
	 * 		Maptile zoom level
	 */
    BoundingBox (final int x, final int y, final int zoom) {
    	this.north = tile2lat(y, zoom);
    	this.south = tile2lat(y + 1, zoom);
    	this.west = tile2lon(x, zoom);
    	this.east = tile2lon(x + 1, zoom);
    }
    
    /**
     * Gets the longitude of Maptile's West boundary
     * @param x	
     * 		Maptile X coordinate
     * @param z
     * 		Maptile zoom level
     * @return
     * 		Longitude
     */
    static double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
     }
    
     /**
      * Gets the latitude of Maptile's North boundary
      * 
      */
     static double tile2lat(int y, int z) {
       double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
       return Math.toDegrees(Math.atan(Math.sinh(n)));
     }
    
     /**
      * Gets North boundary
      * @return
      * 	North boundary latitude
      */
    double getNorth() {
    	return this.north;
    }
    
    /**
     * Gets South boundary
     * @return
     * 		South boundary latitude
     */
    double getSouth() {
    	return this.south;
    }
    
    /**
     * Gets East boundary
     * @return
     * 		East boundary longitude
     */
    double getEast() {
    	return this.east;
    }
    
    /**
     * Gets West boundary
     * @return
     * 		West boundary longitude
     */
    double getWest() {
    	return this.west;
    }
    
    /**
     * Gets Location Bounding box as Comma Separated Values at the following order: West, South, East, North (as used in Twitter's Streaming API)
     * @return
     * 		
     */
    String getWSENCSV() {
    	return this.west + "," + this.south + "," + this.east + "," + this.north;
    }
    
}


