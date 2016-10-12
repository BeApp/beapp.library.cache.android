package fr.beapp.cache.storage;

import java.io.Serializable;

public interface Storage {

	void clear();

	void clear(String keyPrefix);

	void put(String key, Serializable value);

	void del(String key);

	<T extends Serializable> T get(String key, Class<T> clazz);

	<T extends Serializable> T get(String key, Class<T> clazz, T defaultValue);

	boolean exists(String key);

}
