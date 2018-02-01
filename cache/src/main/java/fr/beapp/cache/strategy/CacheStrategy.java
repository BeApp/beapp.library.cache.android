package fr.beapp.cache.strategy;


import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import fr.beapp.cache.internal.CacheWrapper;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

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
	 * Convert the given {@link CacheStrategy} to an {@link Maybe} according to the rules to apply
	 */
	public abstract <T> Flowable<CacheWrapper<T>> getStrategyObservable(@NonNull Maybe<CacheWrapper<T>> cacheObservable, @NonNull Single<CacheWrapper<T>> asyncObservable);

}
