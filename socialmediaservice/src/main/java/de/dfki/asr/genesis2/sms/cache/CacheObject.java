package de.dfki.asr.genesis2.sms.cache;

import java.io.Serializable;

/**
 * CacheObject class
 * 
 * This class transforms <T> class into a Cacheable object by adding Unique ID and Timestamp required for cache management
 *
 * @param <T>
 * 		Class to be cached
 */
public class CacheObject<T> implements Comparable<CacheObject<T>>, Serializable {

	private static final long serialVersionUID = 1L;
	
	private String id;
	private long timestamp;
	private T obj;
	
	/**
	 * CacheObject constructor
	 * @param id
	 * 		The POI id
	 * @param timestamp
	 * 		POI timestamp, required for sorting and eviction
	 * @param obj
	 * 		Object to be cached
	 */
	public CacheObject(String id, long timestamp, T obj) {
		this.id = id;
		this.timestamp = timestamp;
		this.obj = obj;
	}
	
	/**
	 * Gets CacheObject ID
	 * @return
	 * 		CacheObject ID
	 */
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * Gets CacheObject Timestamp (Unix time in milliseconds)
	 * @return
	 * 		CacheObject Timestamp (Unix time in milliseconds)
	 */
	public long getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(long timsetamp) {
		this.timestamp = Math.max(timsetamp, this.timestamp);
	}
	
	/**
	 * Gets Object
	 * @return
	 * 		CacheObject Timestamp (Unix time in milliseconds)
	 */
	public T getObject() {
		return obj;
	}
	
	public void setObject(T obj) {
		this.obj = obj;
	}
	
	/**
	 * CacheObject is equal if it has same ID
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		CacheObject<T> obj = (CacheObject<T>) o;
		return this.id.equals(obj.id);
	}

	/**
	 * Compares with another CacheObject based on timestamp
	 */
	@Override
	public int compareTo(CacheObject<T> arg0) {
		// TODO Auto-generated method stub
		return Double.compare(this.timestamp, arg0.timestamp);
	}
	
	
	
}
