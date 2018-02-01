package fr.beapp.cache.strategy;


import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import fr.beapp.cache.internal.CacheWrapper;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class CacheOrAsyncStrategy extends CacheStrategy {

	public static final long DEFAULT_TTL_VALUE = 30;
	public static final TimeUnit DEFAULT_TTL_TIME_UNIT = TimeUnit.MINUTES;

	protected boolean keepExpiredCache = false;
	protected long ttlValue = DEFAULT_TTL_VALUE;
	protected TimeUnit ttlTimeUnit = DEFAULT_TTL_TIME_UNIT;

	public CacheOrAsyncStrategy() {
		// Just use default values
	}

	public CacheOrAsyncStrategy(boolean keepExpiredCache, long ttlValue, TimeUnit ttlTimeUnit) {
		this.keepExpiredCache = keepExpiredCache;
		this.ttlValue = ttlValue;
		this.ttlTimeUnit = ttlTimeUnit;
	}

	@Override
	public <T> Flowable<CacheWrapper<T>> getStrategyObservable(@NonNull final Maybe<CacheWrapper<T>> cacheObservable, @NonNull final Single<CacheWrapper<T>> asyncObservable) {
		return cacheObservable
				.filter(new Predicate<CacheWrapper<T>>() {
					@Override
					public boolean test(@io.reactivex.annotations.NonNull CacheWrapper<T> cacheWrapper) throws Exception {
						return isValid(cacheWrapper.getCachedDate());
					}
				})
				.switchIfEmpty(asyncObservable
						.onErrorResumeNext(new Function<Throwable, SingleSource<? extends CacheWrapper<T>>>() {
							@Override
							public SingleSource<? extends CacheWrapper<T>> apply(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
								return cacheObservable
										.switchIfEmpty(Maybe.<CacheWrapper<T>>error(throwable))
										.toSingle();
							}
						}).toMaybe())
				.toFlowable();
	}

	public CacheOrAsyncStrategy keepExpiredCache(boolean keepExpiredCache) {
		this.keepExpiredCache = keepExpiredCache;
		return this;
	}

	public CacheOrAsyncStrategy ttlValue(long ttlValue, TimeUnit ttlTimeUnit) {
		this.ttlValue = ttlValue;
		this.ttlTimeUnit = ttlTimeUnit;
		return this;
	}

	private boolean isValid(long cacheDate) {
		return keepExpiredCache || System.currentTimeMillis() < cacheDate + TimeUnit.MILLISECONDS.convert(ttlValue, ttlTimeUnit);
	}

}
