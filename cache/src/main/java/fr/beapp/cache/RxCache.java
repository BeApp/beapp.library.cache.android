package fr.beapp.cache;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import fr.beapp.cache.internal.CacheWrapper;
import fr.beapp.cache.storage.SnappyDBStorage;
import fr.beapp.cache.storage.Storage;
import fr.beapp.cache.strategy.CacheStrategy;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * This class is the entry point of the Cache management.
 * <br/>
 */
public class RxCache {

	protected final Storage storage;

	protected String defaultSessionName = null;
	protected long defaultTTLValue = 30;
	protected TimeUnit defaultTTLTimeUnit = TimeUnit.MINUTES;

	/**
	 * Initialize the cache with {@link SnappyDBStorage} as storage implementation.
	 *
	 * @param context The app's context
	 */
	public RxCache(@NonNull Context context) {
		this(new SnappyDBStorage(context));
	}

	/**
	 * Initialize the cache with the given {@link Storage} implementation.
	 *
	 * @param storage The implementation to use to store data
	 */
	public RxCache(@NonNull Storage storage) {
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

	/**
	 * Create a new builder to configure data cache resolution strategy for the given key.
	 *
	 * @param key  The key pattern to retrieve data from {@link Storage}
	 * @param args The arguments to inject in the given key pattern
	 * @return A builder to prepare cache resolution
	 */
	public <T> StrategyBuilder<T> fromKey(@NonNull String key, Object... args) {
		return new StrategyBuilder<>(this, key, args);
	}

	public static class StrategyBuilder<T> {
		protected final String key;
		protected final Object[] args;
		protected final Storage storage;

		protected long ttlValue;
		protected TimeUnit ttlTimeUnit;
		protected String sessionName;
		protected CacheStrategy cacheStrategy = null;
		protected boolean keepExpiredCache = false;
		protected Observable<T> asyncObservable = Observable.empty();

		public StrategyBuilder(@NonNull RxCache rxCache, @NonNull final String key, Object... args) {
			this.key = key;
			this.args = args;
			this.storage = rxCache.getStorage();
			this.ttlValue = rxCache.getDefaultTTLValue();
			this.ttlTimeUnit = rxCache.getDefaultTTLTimeUnit();
			this.sessionName = rxCache.getDefaultSessionName();
		}

		/**
		 * Apply the strategy to use
		 */
		public StrategyBuilder<T> withStrategy(@NonNull CacheStrategy cacheStrategy) {
			this.cacheStrategy = cacheStrategy;
			return this;
		}

		/**
		 * Apply the TTL (Time-To-Live) on the cached data. If data creation date exceeds this TTL, it will be considered expired
		 */
		public StrategyBuilder<T> withTTL(long value, @NonNull TimeUnit timeUnit) {
			this.ttlValue = value;
			this.ttlTimeUnit = timeUnit;
			return this;
		}

		/**
		 * The session to use with the key. This allows us to isolate data from different sessions
		 */
		public StrategyBuilder<T> withSession(@Nullable String sessionName) {
			this.sessionName = sessionName;
			return this;
		}

		/**
		 * The {@link Observable} to use for async operations.
		 */
		public StrategyBuilder<T> withAsync(@Nullable Observable<T> asyncObservable) {
			if (asyncObservable == null) {
				asyncObservable = Observable.empty();
			}
			this.asyncObservable = asyncObservable;
			return this;
		}

		/**
		 * Configure this cache resolution to keep expired data
		 */
		public StrategyBuilder<T> keepExpiredCache() {
			this.keepExpiredCache = true;
			return this;
		}

		/**
		 * Configure this cache resolution to ignore expired data
		 */
		public StrategyBuilder<T> ignoreExpiredCache() {
			this.keepExpiredCache = false;
			return this;
		}

		/**
		 * Convert this resolution data strategy to a Rx {@link Observable}
		 */
		public Observable<T> toObservable() {
			final String prependedKey = sessionName != null ? sessionName + key : key;

			final Observable<CacheWrapper<T>> asyncObservableCaching = asyncObservable
					.map(new Func1<T, CacheWrapper<T>>() {
						@Override
						public CacheWrapper<T> call(T value) {
							return new CacheWrapper<>(value);
						}
					})
					.doOnNext(new Action1<CacheWrapper<T>>() {
						@Override
						public void call(CacheWrapper<T> value) {
							if (value != null) {
								storage.put(prependedKey, value);
							}
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

			if (cacheStrategy == null) {
				cacheStrategy = CacheStrategy.cacheOrAsync(keepExpiredCache, ttlValue, ttlTimeUnit);
			}

			return cacheStrategy.getStrategyObservable(cacheObservable, asyncObservableCaching)
					.subscribeOn(Schedulers.io())
					.map(new Func1<CacheWrapper<T>, T>() {
						@Override
						public T call(CacheWrapper<T> cacheWrapper) {
							return cacheWrapper.getData();
						}
					});
		}

	}
}
