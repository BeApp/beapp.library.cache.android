package fr.beapp.cache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import fr.beapp.cache.storage.Storage;

public class InMemoryStorage implements Storage {

	private Map<String, Serializable> cache = new HashMap<>();

	@Override
	public void clear() {
		cache.clear();
	}

	@Override
	public void clear(String keyPrefix) {
		for (String key : cache.keySet()) {
			if (key.startsWith(keyPrefix)) {
				cache.remove(key);
			}
		}
	}

	@Override
	public void put(String key, Serializable value) {
		cache.put(key, value);
	}

	@Override
	public void del(String key) {
		cache.remove(key);
	}

	@Override
	public <T extends Serializable> T get(String key, Class<T> clazz) {
		return get(key, clazz, null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Serializable> T get(String key, Class<T> clazz, T defaultValue) {
		if (cache.containsKey(key)) {
			return (T) cache.get(key);
		}
		return defaultValue;
	}

	@Override
	public boolean exists(String key) {
		return key.contains(key);
	}

}
