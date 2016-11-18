package fr.beapp.cache.strategy;


import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import fr.beapp.cache.internal.CacheWrapper;
import rx.Observable;

public abstract class CacheStrategy {

	private static CacheStrategy asyncOrCacheStrategy;
	private static CacheStrategy cacheThenAsync;
	private static CacheStrategy justAsync;
	private static CacheStrategy noCache;

	public static CacheStrategy asyncOrCache() {
		if (asyncOrCacheStrategy == null) {
			asyncOrCacheStrategy = new AsyncOrCacheStrategy();
		}
		return asyncOrCacheStrategy;
	}

	public static CacheStrategy cacheOrAsync() {
		return new CacheOrAsyncStrategy();
	}

	public static CacheStrategy cacheOrAsync(boolean keepExpiredCache, long ttlValue, TimeUnit ttlTimeUnit) {
		return new CacheOrAsyncStrategy(keepExpiredCache, ttlValue, ttlTimeUnit);
	}

	public static CacheStrategy cacheThenAsync() {
		if (cacheThenAsync == null) {
			cacheThenAsync = new CacheThenAsyncStrategy();
		}
		return cacheThenAsync;
	}

	public static CacheStrategy justCache() {
		if (justAsync == null) {
			justAsync = new JustCacheStrategy();
		}
		return justAsync;
	}

	public static CacheStrategy noCache() {
		if (noCache == null) {
			noCache = new NoCacheStrategy();
		}
		return noCache;
	}

	/**
	 * Convert the given {@link CacheStrategy} to an {@link Observable} according to the rules to apply
	 */
	public abstract <T> Observable<CacheWrapper<T>> getStrategyObservable(@NonNull Observable<CacheWrapper<T>> cacheObservable, @NonNull Observable<CacheWrapper<T>> asyncObservable);

}
