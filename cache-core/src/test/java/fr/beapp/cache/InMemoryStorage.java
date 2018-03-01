package fr.beapp.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import fr.beapp.cache.storage.Storage;

public class InMemoryStorage implements Storage {

	private Map<String, CacheWrapper<?>> cache = new HashMap<>();

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public void clear() {
		cache.clear();
	}

	@Override
	public synchronized void clear(@NotNull String... sessions) {
		for (String session : sessions) {
			clear(session, "");
		}
	}

	@Override
	public void clear(@Nullable String session, @NotNull String keyPrefix) {
		for (String key : cache.keySet()) {
			if (key.startsWith(buildKey(session, keyPrefix))) {
				cache.remove(key);
			}
		}
	}

	@Override
	public <T> void put(@Nullable String session, @NotNull String key, @Nullable CacheWrapper<T> value) {
		cache.put(buildKey(session, key), value);
	}

	@Override
	public void delete(@Nullable String session, @NotNull String key) {
		cache.remove(buildKey(session, key));
	}

	@Nullable
	@Override
	public <T> CacheWrapper<T> get(@Nullable String session, @NotNull String key, @NotNull Class<T> clazz) {
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		String finalKey = buildKey(session, key);
		if (cache.containsKey(finalKey)) {
			return (CacheWrapper<T>) cache.get(finalKey);
		}
		return null;
	}

	@NotNull
	@Override
	@SuppressWarnings("unchecked")
	public <T> CacheWrapper<T> get(@Nullable String session, @NotNull String key, @NotNull Class<T> clazz, @NotNull T defaultValue) {
		CacheWrapper<T> value = get(session, key, clazz);
		return value != null ? value : new CacheWrapper(defaultValue);
	}

	@Override
	public boolean exists(@Nullable String session, @NotNull String key) {
		return cache.containsKey(buildKey(session, key));
	}

	protected String buildKey(@Nullable String session, @NotNull String key) {
		return session != null && !session.isEmpty() ? session + "_" + key : "global" + key;
	}

}
