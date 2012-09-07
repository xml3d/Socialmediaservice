package de.dfki.asr.genesis2.sms.util;

import java.util.ArrayList;

/**
 * Maptile Class
 * 
 * @author Andre de Oliveira Melo
 *
 */
public class MapTile {
	private int x;
	private int y;
	private int zoom;
	
	/**
	 * Maptile constructor
	 * @param x
	 * 		The x coordinate [0..(2^zoom)-1]
	 * @param y
	 * 		The y coordinate [0..(2^zoom)-1]
	 * @param zoom
	 * 		The zoom level
	 */
	public MapTile (int x, int y, int zoom) {
		this.setX(x);
		this.setY(y);
		this.setZoom(zoom);
	}
	
	/**
	 * Extracts a ArrayList of tiles from a string with concatenated tiles
	 * @param param
	 * 		String of concatenated tile separated by comma,  example: param = "X1,Y1,Z1,X2,Y2,Z2,...,Xn,Yn,Zn"
	 * @return
	 * 		ArrayList of Maptiles extracted from string
	 */
	public static ArrayList<MapTile> extractTiles(String param) {
		if (param == null || param == "") {
			return null;
		}
		String[] tilesArray = param.split(",");
		ArrayList<MapTile> tiles = new ArrayList<MapTile>();
		for (int i=0; i<(tilesArray.length-2); i+=3) {
			int x = Integer.parseInt(tilesArray[i]);
			int y = Integer.parseInt(tilesArray[i+1]);
			int z = Integer.parseInt(tilesArray[i+2]);
			tiles.add(new MapTile(x,y,z));
		}
		return (tiles.size()>0)? tiles: null;
	}
	
	@Override 
	public String toString() {
		return this.x + "," + this.y + "," + this.zoom;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof MapTile))return false;
		MapTile tile = (MapTile) obj;
		return ((this.x == tile.getX()) && (this.y == tile.getY()) && (this.zoom == tile.getZoom()));
	}
	
	/**
	 * Calculates longitude of Top-Left corner
	 * @return
	 * 		Longitude of Top-Left corner in degrees
	 */
    public double toLongitude() {
        return x / Math.pow(2.0, zoom) * 360.0 - 180;
     }
    
	/**
	 * Calculates latitude of Top-Left corner
	 * @return
	 * 		Latitude of Top-Left corner in degrees
	 */
    public double toLatitude() {
       double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, zoom);
       return Math.toDegrees(Math.atan(Math.sinh(n)));
     }
    
    /**
     * Calculates Top-Left corner point coordinate
     * @return
     * 		Top-Left corner point coordinate
     */
    public Point toCoordinate() {
    	return new Point(this.toLatitude(),this.toLongitude());
    }

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getZoom() {
		return zoom;
	}

	public void setZoom(int zoom) {
		this.zoom = zoom;
	}

}
