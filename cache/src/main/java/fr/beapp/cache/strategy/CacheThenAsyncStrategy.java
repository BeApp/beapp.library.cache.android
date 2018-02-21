package fr.beapp.cache.strategy;


import android.support.annotation.NonNull;

import java.util.Arrays;

import fr.beapp.cache.CacheWrapper;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

public class CacheThenAsyncStrategy extends CacheStrategy {

	public CacheThenAsyncStrategy() {
		super("CACHE_THEN_ASYNC");
	}

	@Override
	public <T> Flowable<CacheWrapper<T>> getStrategyObservable(@NonNull Maybe<CacheWrapper<T>> cacheObservable, @NonNull Single<CacheWrapper<T>> asyncObservable) {
		return Flowable.concatDelayError(Arrays.asList(
				cacheObservable.toFlowable(),
				asyncObservable.toFlowable()
		));
	}

}
