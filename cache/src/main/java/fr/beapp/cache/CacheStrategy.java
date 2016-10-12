package fr.beapp.cache;

public enum CacheStrategy {
	/**
	 * Retrieve cached data first then execute async operation, no matters what
	 */
	CACHE_THEN_ASYNC,
	/**
	 * Try to retrieve cached data first. If nothing was found or data was expired, execute async operation
	 */
	CACHE_OR_ASYNC,
	/**
	 * Only retrieve data from cache.
	 */
	JUST_CACHE,
	/**
	 * Only execute async operation
	 */
	NO_CACHE,
	/**
	 * Try to execute async operation first. If the operation failed, retrieve cached data.
	 */
	ASYNC_OR_CACHE
}
