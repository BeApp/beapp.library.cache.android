package fr.beapp.cache.strategy;

import org.jetbrains.annotations.NotNull;

import fr.beapp.cache.CacheWrapper;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;

public class AsyncOrCacheStrategy extends CacheStrategy {

	public AsyncOrCacheStrategy() {
		super("ASYNC_OR_CACHE");
	}

	@Override
	public <T> Flowable<CacheWrapper<T>> getStrategyObservable(@NotNull final Maybe<CacheWrapper<T>> cacheObservable, @NotNull final Single<CacheWrapper<T>> asyncObservable) {
		return asyncObservable
				.onErrorResumeNext(new Function<Throwable, SingleSource<? extends CacheWrapper<T>>>() {
					@Override
					public SingleSource<? extends CacheWrapper<T>> apply(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
						return cacheObservable
								.switchIfEmpty(Maybe.<CacheWrapper<T>>error(throwable))
								.toSingle();
					}
				})
				.toFlowable();
	}

}
