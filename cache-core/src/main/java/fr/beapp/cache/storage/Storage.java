package fr.beapp.cache.storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import fr.beapp.cache.CacheWrapper;

/**
 * A storage is the part of the system which will store and retrieve data from cache.
 */
public interface Storage {

	/**
	 * Close storage
	 */
	void close();

	/**
	 * Clear all data from cache
	 */
	void clear();

	/**
	 * Clear all data from cache for the given session
	 *
	 * @param sessions The sessions to use
	 */
	void clear(@NotNull String... sessions);

	/**
	 * Clear all data from cache with given session and key starting with the given prefix
	 *
	 * @param session   The session to use
	 * @param keyPrefix The key prefix to search
	 */
	void clear(@Nullable String session, @NotNull String keyPrefix);

	/**
	 * Remove a specific data from cache based on the given session and key
	 *
	 * @param session The session to use
	 * @param key     The key to use to remove data
	 */
	void delete(@Nullable String session, @NotNull String key);

	/**
	 * Add a new data in cache with the given session and key
	 *
	 * @param session The session to use
	 * @param key     The key to use to store this data
	 * @param value   The data to add in cache
	 */
	<T> void put(@Nullable String session, @NotNull String key, @Nullable CacheWrapper<T> value);

	/**
	 * Retrieve a data from cache based on the given session and key
	 *
	 * @param session The session to use
	 * @param key     The key to use to retrieve the data
	 * @param clazz   The class on which the data must be casted
	 * @return Actual data if present, <code>CacheWrapper(null)</code> otherwise
	 */
	@Nullable <T> CacheWrapper<T> get(@Nullable String session, @NotNull String key, @NotNull Class<T> clazz);

	/**
	 * Retrieve a data from cache based on the given session and key. If nothing was stored in this key, return the given default value
	 *
	 * @param session      The session to use
	 * @param key          The key to use to retrieve the data
	 * @param clazz        The class on which the data must be casted
	 * @param defaultValue The default value to return in case no data was stored with the given key
	 * @return Actual data if present, <code>CacheWrapper(defaultValue)</code> otherwise
	 */
	@NotNull <T> CacheWrapper<T> get(@Nullable String session, @NotNull String key, @NotNull Class<T> clazz, @NotNull T defaultValue);

	/**
	 * Check if a data was stored with the given session and key
	 *
	 * @param session The session to use
	 * @param key     The key to check
	 * @return <code>true</code> if a data was stored with this key (even if it's null), <code>false</code> otherwise
	 */
	boolean exists(@Nullable String session, @NotNull String key);

}
