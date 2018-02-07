package fr.beapp.cache.storage;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.snappydb.DB;
import com.snappydb.SnappyDB;
import com.snappydb.SnappydbException;

import java.io.File;
import java.io.Serializable;

import fr.beapp.logger.Logger;

/**
 * A {@link Storage} implementation based on <a href="https://github.com/nhachicha/SnappyDB">SnappyDB</a>.
 * <br/>
 * This library relies on <a href="https://github.com/EsotericSoftware/kryo">Kryo</a> in order to provide fast serialization.
 */
public class SnappyDBStorage implements Storage {

	protected final Context context;
	protected DB db;

	public SnappyDBStorage(@NonNull Context context) {
		this.context = context;
	}

	public DB getDb() {
		return getDb(false);
	}

	public synchronized DB getDb(boolean wasForceDeleted) {
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
	 * Open a SNappyDB instance with the given path and name
	 *
	 * @param path         The path where the database will bestored
	 * @param databaseName The database name to use
	 * @return A {@link DB} instance
	 * @throws SnappydbException
	 */
	protected DB openDb(@NonNull String path, @NonNull String databaseName) throws SnappydbException {
		return new SnappyDB.Builder(context)
				.directory(path)
				.name(databaseName)
//						.registerSerializers(DateTime.class, new JodaDateTimeSerializer())
//						.registerSerializers(LocalDate.class, new JodaLocalDateSerializer())
//						.registerSerializers(PeriodType.class, new JodaPeriodTypeSerializer())
				.build();
	}

	/**
	 * Properly close SnappyDB instance
	 */
	public synchronized void closeDb() {
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
	public synchronized void clear(@NonNull String keyPrefix) {
		try {
			String[] keys = getDb().findKeys(keyPrefix);
			for (String key : keys) {
				getDb().del(key);
			}
		} catch (SnappydbException e) {
			Logger.warn("Couldn't clear keys with prefix %s", e, keyPrefix);
		}
	}

	@Override
	public synchronized void put(@NonNull String key, @Nullable Serializable value) {
		try {
			getDb().put(key, value);
		} catch (SnappydbException e) {
			Logger.warn("Data with key %s couldn't be put in cache", e, key);
		}
	}

	@Override
	public synchronized void delete(@NonNull String key) {
		try {
			getDb().del(key);
		} catch (SnappydbException e) {
			Logger.warn("Data with key %s couldn't be deleted from cache", e, key);
		}
	}

	@Nullable
	@Override
	public synchronized <T extends Serializable> T get(@NonNull String key, @NonNull Class<T> clazz) {
		return get(key, clazz, null);
	}

	@Nullable
	@Override
	public synchronized <T extends Serializable> T get(@NonNull String key, @NonNull Class<T> clazz, @Nullable T defaultValue) {
		try {
			if (getDb().exists(key)) {
				return getDb().get(key, clazz);
			} else {
				return defaultValue;
			}
		} catch (SnappydbException e) {
			Logger.warn("Data with key %s couldn't be retrieved from cache. Deleting it", e, key);
			delete(key);
		}
		return defaultValue;
	}

	@Override
	public synchronized boolean exists(@NonNull String key) {
		try {
			return getDb().exists(key);
		} catch (SnappydbException e) {
			Logger.warn("Can't check if there is data for with key %s", e, key);
		}
		return false;
	}

}