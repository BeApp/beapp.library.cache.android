package fr.beapp.cache.strategy;


import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import fr.beapp.cache.internal.CacheWrapper;
import rx.Observable;
import rx.functions.Func1;

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
	public <T> Observable<CacheWrapper<T>> getStrategyObservable(@NonNull final Observable<CacheWrapper<T>> cacheObservable, @NonNull final Observable<CacheWrapper<T>> asyncObservable) {
		return cacheObservable
				.filter(new Func1<CacheWrapper<T>, Boolean>() {
					@Override
					public Boolean call(CacheWrapper<T> cacheWrapper) {
						return isValid(cacheWrapper.getCachedDate());
					}
				})
				.switchIfEmpty(asyncObservable
						.onErrorResumeNext(new Func1<Throwable, Observable<CacheWrapper<T>>>() {
							@Override
							public Observable<CacheWrapper<T>> call(Throwable throwable) {
								return cacheObservable
										.switchIfEmpty(Observable.<CacheWrapper<T>>error(throwable));
							}
						}));
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
