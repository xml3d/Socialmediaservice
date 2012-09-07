package de.dfki.asr.genesis2.sms.cache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;
import org.apache.jcs.engine.control.CompositeCacheManager;

import de.dfki.asr.genesis2.sms.util.SocialNetworkSource;

/**
 * Search Cache
 * 
 * This class contains a individual JCS cache for each Social Network source (Facebook, Twitter, Flickr, Youtube)
 * Each source specific JCS has CacheQueues as elements, and each element has a String key (a concatenation of the relevant search parameters)
 * 
 * 
 * @author Andre de Oliveira Melo
 *
 */
public class SearchCache {
	/**
	 * Facebook JCS Cache
	 */
	private static JCS fbCache;
	
	/**
	 * Twitter JCS Cache
	 */
	private static JCS twCache;
	
	/**
	 * Flickr JCS Cache
	 */
	private static JCS flCache;
	
	/**
	 * Youtube JCS Cache
	 */
	private static JCS ytCache;
	
	/**
	 * CacheQueue maximum capacity
	 */
	private int maxQueueSize = 50;
	
	/**
	 * Search Cache constructor
	 * @param config
	 * 		Cache configurations file path
	 */
	public SearchCache(String config) {
		CompositeCacheManager mgr = CompositeCacheManager.getUnconfiguredInstance();
		Properties props = new Properties();
		try {
			URL url = this.getClass().getClassLoader().getResource(config);
        	InputStream in = url.openStream();
			props.load(in);
			mgr.configure(props);
			fbCache = JCS.getInstance("facebook");
			flCache = JCS.getInstance("flickr");
			twCache = JCS.getInstance("twitter");
			ytCache = JCS.getInstance("youtube");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CacheException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}

	}
	
	/**
	 * Gets Source Cache based on source name
	 * @param source
	 * 		Source name
	 * @return
	 * 		JCS Cache
	 * @throws CacheException
	 * 		if source name is invalid
	 */
	private JCS getCache(SocialNetworkSource source) throws CacheException {
		switch (source) {
			case FACEBOOK: return fbCache; 
			case TWITTER : return twCache; 
			case FLICKR  : return flCache; 
			case YOUTUBE : return ytCache; 	
			default: throw new CacheException("Invalid source (should be one of {facebook, twitter, flickr, youtube})");
		}
	}
	
	/**
	 * Puts a single CacheObject in its correspondent CacheQueue
	 * @param key
	 * 		Cache key (query:coordinate)
	 * @param poi
	 * 		CacheObject to be added (POI XML, ID and timestamp)
	 * @param source
	 * 		Source name to identify Cache
	 * @return
	 * 		true  if CacheObject put successfully
	 * 		false otherwise
	 * @throws CacheException
	 * 		if Source name invalid
	 */
	@SuppressWarnings("unchecked")
	public boolean put(String key, CacheObject<String> poi, SocialNetworkSource source) throws CacheException {	
		JCS cache = getCache(source);
		CacheQueue<String> queue = (CacheQueue<String>) cache.get(key);
		if (queue.contains(poi))
			return false;
		else {
			while (queue.size() >= maxQueueSize) {
				queue.remove();
			}
			return queue.add(poi);
		}		
	}
	
	/**
	 * Puts CacheQueue with given Key into Source Cache 
	 * @param key
	 * 		Cache key (query:coordinate)
	 * @param poiList
	 * 		CacheQueue containing CacheObjects to be added
	 * @param source
	 * 		Source name to identify Cache
	 * @throws CacheException
	 * 		if Source name invalid
	 */
	public void put(String key, CacheQueue<String> poiList, SocialNetworkSource source) throws CacheException {	
		JCS cache = getCache(source);
		if (poiList.getMaxCapacity() != this.maxQueueSize) 
			poiList.setMaxCapacity(maxQueueSize);	
		cache.put(key, poiList);
	}
	
	/**
	 * 
	 * @param key
	 * 		Cache key (query:coordinate)
	 * @param source
	 * 		Source name to identify Cache
	 * @return
	 * 		Timestamp of the last update for the given key (Unix time in milliseconds)
	 * @throws CacheException
	 * 		if Source name invalid
	 */
	@SuppressWarnings("unchecked")
	public long lastUpdate(String key, SocialNetworkSource source) throws CacheException {
		try {
			JCS cache = getCache(source);		
			CacheQueue<String> queue = (CacheQueue<String>) cache.get(key);
			return queue.getLastUpdate();
		}
		catch (NullPointerException e) {
			return -1;
		}
	}

	/**
	 * Gets CacheQueue for the given key at source cache
	 * @param key
	 * 		Cache key (query:coordinate)
	 * @param source
	 * 		Source name to identify Cache
	 * @return
	 * 		CacheQueue for the given key at source cache, or null if not existent
	 * @throws CacheException
	 * 		if Source name invalid
	 */
	@SuppressWarnings("unchecked")
	public CacheQueue<String> get(String key, SocialNetworkSource source) throws CacheException {
		JCS cache = getCache(source);		
		return (CacheQueue<String>) cache.get(key);
	}
	
	/**
	 * Gets copy of retrieved CacheQueue with up to maxResults most recent CacheObjects
	 * @param key
	 * 		Cache key (query:coordinate)
	 * @param maxResults
	 * 		Maximum size of CacheQueue to be returned
	 * @param source
	 * 		Source name to identify Cache
	 * @return
	 * 		Copy of correspondent retrieved CacheQueue with up to maxResults most recent CacheObjects, or null if not existent
	 * @throws CacheException
	 * 		if Source name invalid
	 */
	public CacheQueue<String> get(String key, int maxResults, SocialNetworkSource source) throws CacheException {	
		CacheQueue<String> result = new CacheQueue<String>(maxQueueSize);
		CacheQueue<String> retrieved = get(key, source);
		if (retrieved != null) {
			Iterator<CacheObject<String>> iterator = retrieved.iterator();		
			while (retrieved.size() > maxResults) {
				iterator.next();
				maxResults++;
			}
			while (iterator.hasNext())  {
				result.add(iterator.next());
			}
			return result;
		}
		else
			return null;
	}
	
	/**
	 * Gets string with concatenation of of the XML from top-maxResults most recent CacheObjects in the retrieved CacheQueue. 
	 * @param key
	 * 		Cache key (query:coordinate)
	 * @param maxResults
	 * 		Maximum size of CacheQueue to be returned
	 * @param source
	 * 		Source name to identify Cache
	 * @return
	 * 		Concatenation of the XML from top-maxResults most recent CacheObjects in the retrieved CacheQueue.
	 * @throws CacheException
	 * 		if Source name invalid
	 */
	public String getAsString(String key, int maxResults, SocialNetworkSource source) throws CacheException {	
		String poixml = "";
		Iterator<CacheObject<String>> iterator = get(key, source).iterator();
		int count = 0;
		while (iterator.hasNext() && count++ < maxResults)  {
			poixml += iterator.next().getObject();
		}
		return poixml;
	}
}
;