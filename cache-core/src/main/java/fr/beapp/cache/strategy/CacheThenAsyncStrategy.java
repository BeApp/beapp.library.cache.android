package fr.beapp.cache.strategy;

import org.jetbrains.annotations.NotNull;

import fr.beapp.cache.CacheWrapper;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

public class CacheThenAsyncStrategy extends CacheStrategy {

	public CacheThenAsyncStrategy() {
		super("CACHE_THEN_ASYNC");
	}

	@Override
	public <T> Flowable<CacheWrapper<T>> getStrategyObservable(@NotNull Maybe<CacheWrapper<T>> cacheObservable, @NotNull Single<CacheWrapper<T>> asyncObservable) {
		return cacheObservable
				.concatWith(asyncObservable.toMaybe());
	}

}
