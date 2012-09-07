package de.dfki.asr.genesis2.sms.cache;

import java.io.Serializable;
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.SortedSet;


/**
 * CacheQueue class
 * 
 * This class is a PriorityQueue of CacheObjects and correspond to a JCS cache element
 * The PriorityQueue is responsible to manage the multiple CacheObjects associated to the same JCS cache key.
 * CacheObject Timestamp is used to control eviction (when CacheQueue's maximum capacity is exceeded)
 * CacheObject ID is used to control unicity. A CacheQueue should not contain two CacheObjects with same ID 
 * 
 * @author Andre de Oliveira Melo
 * 
 * @param <T>
 *		Class to be cached
 */
public class CacheQueue<T> extends PriorityQueue<CacheObject<T>> implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private static final int defaultMaxCapacity = 50;

	private long lastUpdate = -1;
	private int maxCapacity = -1;
	private String maxId = null;
	
	/**
	 * 	Contructs empty CacheQueue with default maximum capacity
	 */
	public CacheQueue() { 
		this(defaultMaxCapacity); 
	}
	/**
	 * Contructs CacheQueue with elements from SortedSet and with default maximum capacity
	 * @param c
	 * 		SortedSet with elements to be added to added to CacheQueue
	 */
	public CacheQueue(SortedSet<CacheObject<T>> c) { 
		this(c,defaultMaxCapacity); 
	}
	/**
	 * Contructs CacheQueue with elements from SortedSet and with default maximum capacity
	 * @param c
	 * 		Collection with elements to be added to added to CacheQueue
	 */
	public CacheQueue(Collection<CacheObject<T>> c) { 
		this(c,defaultMaxCapacity); 
	}
	/**
	 * @param maxCapacity
	 * 		Maximum capacity of Cache Queue
	 */
	public CacheQueue(int maxCapacity) {
		super(maxCapacity);
		this.maxCapacity = maxCapacity;
		calculateLastUpdateAndMaxId();
	}
	
	/**
	 * Contructs CacheQueue with elements from SortedSet and with given maximum capacity
	 * @param c
	 * 		SortedSet with elements to be added to added to CacheQueue
	 * @param maxCapacity
	 * 		Maximum capacity of Cache Queue
	 */
	public CacheQueue(SortedSet<CacheObject<T>> c, int maxCapacity) {
		super(c);
		this.maxCapacity = maxCapacity;
		calculateLastUpdateAndMaxId();
	}
	
	/**
	 * Contructs CacheQueue with elements from SortedSet and with given maximum capacity
	 * @param c
	 * 		Collection with elements to be added to added to CacheQueue
	 * @param maxCapacity
	 * 		Maximum capacity of Cache Queue
	 */
	public CacheQueue(Collection<CacheObject<T>> c, int maxCapacity) {
		super(c);
		this.maxCapacity = maxCapacity;
		calculateLastUpdateAndMaxId();
	}
	
	/**
	 * @return
	 * 		Last update timestamp (Unix time in milliseconds)
	 */
	public long getLastUpdate() {
		return this.lastUpdate;
	}
	
	/**
	 * Explicitely sets the lastUpdate timestamp. Adding elements with more recent recent timestamp will change it automatically
	 * @param lastUpdate
	 * 		Last update timestamp (Unix time in milliseconds)
	 */
	public void setLastUpdate(long lastUpdate) {
		if (lastUpdate > this.lastUpdate)
			this.lastUpdate = lastUpdate;
	}
	
	/**
	 * Gets the ID of latest element in CacheQueue 
	 * @return
	 * 		ID of latest element in CacheQueue
	 */
	public String getMaxId() {
		return maxId;
	}
	
	/**
	 * Explicitely sets the maxID. Adding elements with more recent recent timestamp will change it automatically
	 * @param maxId
	 * 		ID of latest element in CacheQueue
	 */
	public void setMaxId(String maxId) {
		this.maxId = maxId;
	}
	
	/**
	 * Adds a CacheObject to CachedQueue guaranteeing it wont exceed its maximum capacity
	 * @see java.util.PriorityQueue#add(java.lang.Object)
	 */
	@Override
	public boolean add(CacheObject<T> c) {
		// TODO Auto-generated method stub
		if (c==null)
			return false;
		if (!this.contains(c)) {
			if (c.getTimestamp() > lastUpdate) {
				lastUpdate = c.getTimestamp();
				maxId = c.getId();
			}
			boolean success = super.add(c);
			controlSize();
			return success;
		}
		return false;
		
	}

	/**
	 * Adds all elements from collection if not already present in the CacheQueue and controls size
	 * If CacheQueue exceeds maximum capacity after addition, elements with the smallest timestamps are eliminated.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean addAll(Collection<? extends CacheObject<T>> c) {
		// TODO Auto-generated method stub
		if (c==null)
			return false;
		CacheQueue<T> queue = (CacheQueue<T>) c;
		boolean success = super.addAll(queue);
		if (queue.getLastUpdate() > lastUpdate) {
			lastUpdate = queue.lastUpdate;
			maxId = queue.getMaxId();
		}
		controlSize();
		return success;
	}
	
	/**
	 * Guarantees that the CacheQueue size doesn't exceed the maximum capacity by elimintating elements with the smallest timestamps
	 */
	private void controlSize() {
		while (this.size() > maxCapacity) {
			this.poll();
		}
	}

	/**
	 * Scans the Queue content and finds LastUpdate and MaxID
	 * Required for contructors with non-empty initialization
	 */
	private void calculateLastUpdateAndMaxId() {
		for (CacheObject<T> o: this) {
			if (o.getTimestamp() > lastUpdate) {
				lastUpdate = o.getTimestamp();
				maxId = o.getId();
			}
		}
	}
	
	/**
	 * Gets maximum capacity
	 * @return
	 * 		Maximum Capacity
	 */
	public int getMaxCapacity() {
		return this.maxCapacity;
	}
	
	/**
	 * Changes CacheQueue maximum capacity, deleting elements if its size is greater than new maximum capacity 
	 * @param maxCapacity
	 * 		Maximum Capacity (greater than 0)
	 */
	public void setMaxCapacity(int maxCapacity) {
		if (maxCapacity <= 0)
			throw new IllegalArgumentException("Maximum capacity="+maxCapacity+", it should be greater than 0");
		this.maxCapacity = maxCapacity;
		if (this.maxCapacity < this.size())
			controlSize();
	}
}

