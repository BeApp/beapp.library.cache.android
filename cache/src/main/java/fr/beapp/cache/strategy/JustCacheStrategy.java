package fr.beapp.cache.strategy;


import android.support.annotation.NonNull;

import fr.beapp.cache.internal.CacheWrapper;
import rx.Observable;

public class JustCacheStrategy extends CacheStrategy {

	@Override
	public <T> Observable<CacheWrapper<T>> getStrategyObservable(@NonNull Observable<CacheWrapper<T>> cacheObservable, @NonNull Observable<CacheWrapper<T>> asyncObservable) {
		return cacheObservable;
	}

}
