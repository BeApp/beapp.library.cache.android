package fr.beapp.cache;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import fr.beapp.cache.storage.SnappyDBStorage;
import fr.beapp.cache.storage.Storage;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class RxCache {

	protected final Storage storage;

	protected String sessionName;
	protected long ttlValue = 30;
	protected TimeUnit ttlTimeUnit = TimeUnit.MINUTES;

	public RxCache(Context context) {
		this(new SnappyDBStorage(context));
	}

	public RxCache(Storage storage) {
		this.storage = storage;
	}

	public RxCache withTTL(long value, @NonNull TimeUnit timeUnit) {
		this.ttlValue = value;
		this.ttlTimeUnit = timeUnit;
		return this;
	}

	public RxCache withSession(@NonNull String sessionName) {
		this.sessionName = sessionName;
		return this;
	}

	public String buildKey(String key, Object... args) {
		return String.format(Locale.ENGLISH, key, args);
	}

	/**
	 * Handle cache for the requested data. The asyncObservable should return a list of data if needed
	 */
	public <T> Observable<T> executeRx(final String key, CacheStrategy strategy, Observable<T> asyncObservable) {
		final String prependedKey = prependKey(key);

		final Observable<T> asyncObservableCaching = asyncObservable.compose(new Observable.Transformer<T, T>() {
			@Override
			public Observable<T> call(Observable<T> observable) {
				return observable.doOnNext(new Action1<T>() {
					@Override
					public void call(T value) {
						if (value != null) {
							storage.put(prependedKey, new CacheWrapper<>(value));
						}
					}
				});
			}
		});

		final Observable<CacheWrapper<T>> cacheObservable = Observable.create(new Observable.OnSubscribe<CacheWrapper<T>>() {
			@Override
			@SuppressWarnings("unchecked")
			public void call(Subscriber<? super CacheWrapper<T>> subscriber) {
				try {
					CacheWrapper<T> cachedData = storage.get(prependedKey, CacheWrapper.class);
					if (cachedData != null) {
						subscriber.onNext(cachedData);
					}
					subscriber.onCompleted();
				} catch (Exception e) {
					subscriber.onError(e);
				}
			}
		});

		return getStrategyObservable(strategy, cacheObservable, asyncObservableCaching)
				.subscribeOn(Schedulers.io());
	}

	protected String prependKey(String key) {
		return sessionName != null ? sessionName + key : key;
	}

	protected boolean isExpired(long cacheDate) {
		return System.currentTimeMillis() > cacheDate + TimeUnit.MILLISECONDS.convert(ttlValue, ttlTimeUnit);
	}

	private <T> Observable<T> getStrategyObservable(final CacheStrategy strategy, final Observable<CacheWrapper<T>> cacheObservable, final Observable<T> asyncObservable) {
		switch (strategy) {
			case CACHE_THEN_ASYNC:
				return cacheObservable
						.map(new Func1<CacheWrapper<T>, T>() {
							@Override
							public T call(CacheWrapper<T> cacheWrapper) {
								return cacheWrapper.getData();
							}
						})
						.concatWith(asyncObservable);
			case ASYNC_IF_NEEDED:
				return cacheObservable
						.filter(new Func1<CacheWrapper<T>, Boolean>() {
							@Override
							public Boolean call(CacheWrapper<T> cacheWrapper) {
								return !isExpired(cacheWrapper.getCachedDate());
							}
						})
						.map(new Func1<CacheWrapper<T>, T>() {
							@Override
							public T call(CacheWrapper<T> cacheWrapper) {
								return cacheWrapper.getData();
							}
						})
						.switchIfEmpty(asyncObservable
								.onErrorResumeNext(new Func1<Throwable, Observable<T>>() {
									@Override
									public Observable<T> call(Throwable throwable) {
										return cacheObservable
												.map(new Func1<CacheWrapper<T>, T>() {
													@Override
													public T call(CacheWrapper<T> cacheWrapper) {
														return cacheWrapper.getData();
													}
												})
												.switchIfEmpty(Observable.<T>error(throwable));
									}
								}));
			case JUST_CACHE:
				return cacheObservable
						.map(new Func1<CacheWrapper<T>, T>() {
							@Override
							public T call(CacheWrapper<T> cacheWrapper) {
								return cacheWrapper.getData();
							}
						});
			case NO_CACHE:
				return asyncObservable;
			case NO_CACHE_BUT_SAVED:
				return asyncObservable.onErrorResumeNext(new Func1<Throwable, Observable<T>>() {
					@Override
					public Observable<T> call(Throwable throwable) {
						return cacheObservable
								.map(new Func1<CacheWrapper<T>, T>() {
									@Override
									public T call(CacheWrapper<T> cacheWrapper) {
										return cacheWrapper.getData();
									}
								})
								.switchIfEmpty(Observable.<T>error(throwable));
					}
				});
		}
		return asyncObservable;
	}
}
