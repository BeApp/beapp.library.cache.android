package fr.beapp.cache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
	public void clear(@NonNull String keyPrefix) {
		for (String key : cache.keySet()) {
			if (key.startsWith(keyPrefix)) {
				cache.remove(key);
			}
		}
	}

	@Override
	public void put(@NonNull String key, @Nullable Serializable value) {
		cache.put(key, value);
	}

	@Override
	public void delete(@NonNull String key) {
		cache.remove(key);
	}

	@Nullable
	@Override
	public <T extends Serializable> T get(@NonNull String key, @NonNull Class<T> clazz) {
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (cache.containsKey(key)) {
			return (T) cache.get(key);
		}
		return null;
	}

	@NonNull
	@Override
	@SuppressWarnings("unchecked")
	public <T extends Serializable> T get(@NonNull String key, @NonNull Class<T> clazz, @NonNull T defaultValue) {
		T value = get(key, clazz);
		return value != null ? value : defaultValue;
	}

	@Override
	public boolean exists(@NonNull String key) {
		return key.contains(key);
	}

}
