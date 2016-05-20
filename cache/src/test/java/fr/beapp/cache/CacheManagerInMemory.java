package fr.beapp.cache;

import android.content.Context;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CacheManagerInMemory extends CacheManager {

	private Map<String, Serializable> cache = new HashMap<>();

	public CacheManagerInMemory(Context context) {
		super(context);
	}

	@Override
	public synchronized void clear() {
		cache.clear();
	}

	@Override
	public synchronized void put(String key, Serializable value) {
		if (!(value instanceof CacheWrapper)) {
			value = new CacheWrapper<>(value);
		}
		cache.put(key, value);
	}

	@Override
	public synchronized void delete(String key) {
		cache.remove(key);
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized <T extends Serializable> T get(String key, Class<T> clazz, T defaultValue) {
		if (cache.containsKey(key)) {
			return (T) cache.get(key);
		}
		return defaultValue;
	}

	@Override
	public synchronized boolean exists(String key) {
		return cache.containsKey(key);
	}

}
