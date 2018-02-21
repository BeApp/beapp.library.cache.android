package fr.beapp.cache.strategy;


import android.support.annotation.NonNull;

import fr.beapp.cache.CacheWrapper;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

public class NoCacheStrategy extends CacheStrategy {

	public NoCacheStrategy() {
		super("NO_CACHE");
	}

	@Override
	public <T> Flowable<CacheWrapper<T>> getStrategyObservable(@NonNull Maybe<CacheWrapper<T>> cacheObservable, @NonNull Single<CacheWrapper<T>> asyncObservable) {
		return asyncObservable.toFlowable();
	}

}
