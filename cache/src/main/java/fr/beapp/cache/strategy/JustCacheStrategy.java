package fr.beapp.cache.strategy;


import android.support.annotation.NonNull;

import fr.beapp.cache.internal.CacheWrapper;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

public class JustCacheStrategy extends CacheStrategy {

	public JustCacheStrategy() {
		super("JUST_CACHE");
	}

	@Override
	public <T> Flowable<CacheWrapper<T>> getStrategyObservable(@NonNull Maybe<CacheWrapper<T>> cacheObservable, @NonNull Single<CacheWrapper<T>> asyncObservable) {
		return cacheObservable.toFlowable();
	}

}
