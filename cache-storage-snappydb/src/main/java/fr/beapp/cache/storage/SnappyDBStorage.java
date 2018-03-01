package fr.beapp.cache.storage;

import android.content.Context;

import com.snappydb.DB;
import com.snappydb.SnappyDB;
import com.snappydb.SnappydbException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import fr.beapp.cache.CacheWrapper;
import fr.beapp.logger.Logger;

/**
 * A {@link Storage} implementation based on <a href="https://github.com/nhachicha/SnappyDB">SnappyDB</a>.
 * <br/>
 * This library relies on <a href="https://github.com/EsotericSoftware/kryo">Kryo</a> in order to provide fast serialization.
 */
public class SnappyDBStorage implements Storage {
	protected final Context context;

	protected DB db;

	public SnappyDBStorage(@NotNull Context context) {
		this.context = context;
	}

	@Override
	public synchronized void close() {
		if (db != null) {
			try {
				Logger.trace("Closing SnappyDB");

				db.close();
				db = null;
			} catch (SnappydbException e) {
				Logger.error("Can't close cache database", e);
			}
		}
	}

	@Override
	public synchronized void clear() {
		try {
			getDb().destroy();
		} catch (SnappydbException e) {
			Logger.warn("Couldn't clear cache", e);
		}
	}

	@Override
	public synchronized void clear(@NotNull String... sessions) {
		for (String session : sessions) {
			clear(session, "");
		}
	}

	@Override
	public synchronized void clear(@Nullable String session, @NotNull String keyPrefix) {
		try {
			String[] keys = getDb().findKeys(buildKey(session, keyPrefix));
			for (String key : keys) {
				getDb().del(key);
			}
		} catch (SnappydbException e) {
			Logger.warn("Couldn't clear keys with prefix %s", e, keyPrefix);
		}
	}

	@Override
	public synchronized <T> void put(@Nullable String session, @NotNull String key, @Nullable CacheWrapper<T> value) {
		String finalKey = buildKey(session, key);
		try {
			getDb().put(key, value);
		} catch (SnappydbException e) {
			Logger.warn("Data with key %s couldn't be put in cache", e, finalKey);
		}
	}

	@Override
	public synchronized void delete(@Nullable String session, @NotNull String key) {
		String finalKey = buildKey(session, key);
		try {
			getDb().del(finalKey);
		} catch (SnappydbException e) {
			Logger.warn("Data with key %s couldn't be deleted from cache", e, finalKey);
		}
	}

	@Nullable
	@Override
	@SuppressWarnings("unchecked")
	public synchronized <T> CacheWrapper<T> get(@Nullable String session, @NotNull String key, @NotNull Class<T> clazz) {
		String finalKey = buildKey(session, key);
		try {
			if (getDb().exists(finalKey)) {
				return getDb().get(finalKey, CacheWrapper.class);
			} else {
				return null;
			}
		} catch (SnappydbException e) {
			Logger.warn("Data with key %s couldn't be retrieved from cache. Deleting it", e, finalKey);
			delete(session, key);
		}
		return null;
	}

	@NotNull
	@Override
	public synchronized <T> CacheWrapper<T> get(@Nullable String session, @NotNull String key, @NotNull Class<T> clazz, @NotNull T defaultValue) {
		CacheWrapper<T> value = get(session, key, clazz);
		return value != null ? value : new CacheWrapper<>(defaultValue);
	}

	@Override
	public synchronized boolean exists(@Nullable String session, @NotNull String key) {
		String finalKey = buildKey(session, key);
		try {
			return getDb().exists(finalKey);
		} catch (SnappydbException e) {
			Logger.warn("Can't check if there is data for with key %s", e, finalKey);
		}
		return false;
	}

	protected DB getDb() {
		return getDb(false);
	}

	protected synchronized DB getDb(boolean wasForceDeleted) {
		if (db != null)
			return db;

		String path = context.getFilesDir().getAbsolutePath() + File.separator + "snappydb";
		String databaseName = "snappydb";

		try {
			Logger.info("Initializing SnappyDB database %s at %s", databaseName, path);

			db = openDb(path, databaseName);
		} catch (Exception e) {
			Logger.error("Can't open cache database. No data will be cached", e);

			// TODO Keep an eye on https://github.com/nhachicha/SnappyDB/issues/42 and implement a proper solution when available
			// For now, we force deleting the database if we received an Exception about corrupted database
			if (!wasForceDeleted && e.getLocalizedMessage().contains("Corruption")) {
				Logger.warn("SnappyDB database seems to be corrupted. Trying to delete it");

				File databaseFile = new File(path, databaseName);
				try {
					if (databaseFile.delete()) {
						Logger.debug("SnappyDB database file deleted from %s", databaseFile);
					}
				} catch (Exception ignored) {
				}

				return getDb(true);
			}
		}
		return db;
	}

	/**
	 * Open a SnappyDB instance with the given path and name
	 *
	 * @param path         The path where the database will be stored
	 * @param databaseName The database name to use
	 * @return A {@link DB} instance
	 * @throws SnappydbException if SnappyDB couldn't be initialized
	 */
	protected DB openDb(@NotNull String path, @NotNull String databaseName) throws SnappydbException {
		return new SnappyDB.Builder(context)
				.directory(path)
				.name(databaseName)
				.build();
	}

	protected String buildKey(@Nullable String session, @NotNull String key) {
		return session != null && !session.isEmpty() ? session + "_" + key : "global" + key;
	}

}