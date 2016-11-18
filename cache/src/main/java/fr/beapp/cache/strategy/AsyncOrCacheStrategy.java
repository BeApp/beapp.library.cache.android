package fr.beapp.cache.strategy;


import android.support.annotation.NonNull;

import fr.beapp.cache.internal.CacheWrapper;
import rx.Observable;
import rx.functions.Func1;

public class AsyncOrCacheStrategy extends CacheStrategy {

	@Override
	public <T> Observable<CacheWrapper<T>> getStrategyObservable(@NonNull final Observable<CacheWrapper<T>> cacheObservable, @NonNull final Observable<CacheWrapper<T>> asyncObservable) {
		return asyncObservable.onErrorResumeNext(new Func1<Throwable, Observable<CacheWrapper<T>>>() {
			@Override
			public Observable<CacheWrapper<T>> call(Throwable throwable) {
				return cacheObservable
						.switchIfEmpty(Observable.<CacheWrapper<T>>error(throwable));
			}
		});
	}

}
