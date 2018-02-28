package fr.beapp.cache.strategy;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import fr.beapp.cache.CacheWrapper;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

public abstract class CacheStrategy {
	private static CacheStrategy asyncOrCacheStrategy;
	private static CacheStrategy cacheThenAsync;
	private static CacheStrategy justAsync;
	private static CacheStrategy noCache;

	private final String name;

	protected CacheStrategy(@NotNull String name) {
		this.name = name;
	}

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

	@Override
	public String toString() {
		return name;
	}

	public String getName() {
		return name;
	}

	/**
	 * Convert the given {@link CacheStrategy} to an {@link Maybe} according to the rules to apply
	 */
	public abstract <T> Flowable<CacheWrapper<T>> getStrategyObservable(@NotNull Maybe<CacheWrapper<T>> cacheObservable, @NotNull Single<CacheWrapper<T>> asyncObservable);

}
