package fr.beapp.cache.storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * A storage is the part of the system which will store and retrieve data from cache.
 */
public interface Storage {

	/**
	 * Clear all data from cache
	 */
	void clear();

	/**
	 * Clear all data from cache with key starting with the given prefix
	 *
	 * @param keyPrefix The key prefix to search
	 */
	void clear(@NotNull String keyPrefix);

	/**
	 * Remove a specific data from cache based on the given key
	 *
	 * @param key The key to use to remove data
	 */
	void delete(@NotNull String key);

	/**
	 * Add a new data in cache
	 *
	 * @param key   The key to use to store this data
	 * @param value The data to add in cache
	 */
	void put(@NotNull String key, @Nullable Serializable value);

	/**
	 * Retrieve a data from cache based on the given key
	 *
	 * @param key   The key to use to retrieve the data
	 * @param clazz The class on which the data must be casted
	 * @return Actual data if present, <code>null</code> otherwise
	 */
	@Nullable <T extends Serializable> T get(@NotNull String key, @NotNull Class<T> clazz);

	/**
	 * Retrieve a data from cache based on the given key. If nothing was stored in this key, return the given defualt value
	 *
	 * @param key          The key to use to retrieve the data
	 * @param clazz        The class on which the data must be casted
	 * @param defaultValue The default value to return in case no data was stored with the given key
	 * @return Actual data if present, <code>defaultValue</code> otherwise
	 */
	@NotNull <T extends Serializable> T get(@NotNull String key, @NotNull Class<T> clazz, @NotNull T defaultValue);

	/**
	 * Check if a data was stored with the given key
	 *
	 * @param key The key to check
	 * @return <code>true</code> if a data was stored with this key (even if it's null), <code>false</code> otherwise
	 */
	boolean exists(@NotNull String key);

}
