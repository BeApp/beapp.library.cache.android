package fr.beapp.cache;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import fr.beapp.cache.internal.CacheWrapper;
import fr.beapp.cache.storage.SnappyDBStorage;
import fr.beapp.cache.storage.Storage;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class RxCache {

	protected final Storage storage;

	protected String defaultSessionName = null;
	protected long defaultTTLValue = 30;
	protected TimeUnit defaultTTLTimeUnit = TimeUnit.MINUTES;

	public RxCache(Context context) {
		this(new SnappyDBStorage(context));
	}

	public RxCache(Storage storage) {
		this.storage = storage;
	}

	public Storage getStorage() {
		return storage;
	}

	public long getDefaultTTLValue() {
		return defaultTTLValue;
	}

	public TimeUnit getDefaultTTLTimeUnit() {
		return defaultTTLTimeUnit;
	}

	public RxCache withDefaultTTL(long value, @NonNull TimeUnit timeUnit) {
		this.defaultTTLValue = value;
		this.defaultTTLTimeUnit = timeUnit;
		return this;
	}

	public String getDefaultSessionName() {
		return defaultSessionName;
	}

	public RxCache withDefaultSession(@NonNull String sessionName) {
		this.defaultSessionName = sessionName;
		return this;
	}

	public <T> StrategyBuilder<T> fromKey(String key, Object... args) {
		return new StrategyBuilder<>(this, key, args);
	}

	public static class StrategyBuilder<T> {
		protected final String key;
		protected final Object[] args;
		protected final Storage storage;

		protected long ttlValue;
		protected TimeUnit ttlTimeUnit;
		protected String sessionName;
		protected CacheStrategy cacheStrategy = CacheStrategy.ASYNC_IF_NEEDED;
		protected boolean keepExpiredCache = false;
		protected Observable<T> asyncObservable = Observable.empty();

		public StrategyBuilder(RxCache rxCache, final String key, Object... args) {
			this.key = key;
			this.args = args;
			this.storage = rxCache.getStorage();
			this.ttlValue = rxCache.getDefaultTTLValue();
			this.ttlTimeUnit = rxCache.getDefaultTTLTimeUnit();
			this.sessionName = rxCache.getDefaultSessionName();
		}

		public StrategyBuilder<T> withStrategy(CacheStrategy cacheStrategy) {
			this.cacheStrategy = cacheStrategy;
			return this;
		}

		public StrategyBuilder<T> withTTL(long value, TimeUnit timeUnit) {
			this.ttlValue = value;
			this.ttlTimeUnit = timeUnit;
			return this;
		}

		public StrategyBuilder<T> withSession(String sessionName) {
			this.sessionName = sessionName;
			return this;
		}

		public StrategyBuilder<T> withAsync(Observable<T> asyncObservable) {
			this.asyncObservable = asyncObservable;
			return this;
		}

		public StrategyBuilder<T> keepExpiredCache() {
			this.keepExpiredCache = true;
			return this;
		}

		public StrategyBuilder<T> ignoreExpiredCache() {
			this.keepExpiredCache = false;
			return this;
		}

		public Observable<T> toObservable() {
			final String prependedKey = sessionName != null ? sessionName + key : key;

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

			return getStrategyObservable(cacheStrategy, cacheObservable, asyncObservableCaching)
					.subscribeOn(Schedulers.io());
		}

		protected Observable<T> getStrategyObservable(final CacheStrategy strategy, final Observable<CacheWrapper<T>> cacheObservable, final Observable<T> asyncObservable) {
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

		protected boolean isExpired(long cacheDate) {
			return System.currentTimeMillis() > cacheDate + TimeUnit.MILLISECONDS.convert(ttlValue, ttlTimeUnit);
		}
	}
}
