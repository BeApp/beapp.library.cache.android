package fr.beapp.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import fr.beapp.cache.storage.Storage;
import fr.beapp.cache.strategy.CacheStrategy;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * This class is the entry point of the Cache management.
 * <br/>
 */
public class RxCache {

	protected final Storage storage;

	protected String defaultSessionName = null;
	protected long defaultTTLValue = 30;
	protected TimeUnit defaultTTLTimeUnit = TimeUnit.MINUTES;
	protected Scheduler defaultScheduler = Schedulers.io();

	/**
	 * Initialize the cache with the given {@link Storage} implementation.
	 *
	 * @param storage The implementation to use to store data
	 */
	public RxCache(@NotNull Storage storage) {
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

	public RxCache withDefaultTTL(long value, @NotNull TimeUnit timeUnit) {
		this.defaultTTLValue = value;
		this.defaultTTLTimeUnit = timeUnit;
		return this;
	}

	public String getDefaultSessionName() {
		return defaultSessionName;
	}

	public RxCache withDefaultSession(@Nullable String sessionName) {
		this.defaultSessionName = sessionName;
		return this;
	}

	public Scheduler getDefaultScheduler() {
		return defaultScheduler;
	}

	public RxCache withDefaultScheduler(@NotNull Scheduler scheduler) {
		this.defaultScheduler = scheduler;
		return this;
	}

	/**
	 * Create a new builder to configure data cache resolution strategy for the given key.
	 *
	 * @param key  The key pattern to retrieve data from {@link Storage}
	 * @param args The arguments to inject in the given key pattern
	 * @return A builder to prepare cache resolution
	 */
	public <T> StrategyBuilder<T> fromKey(@NotNull String key, Object... args) {
		return new StrategyBuilder<>(this, key, args);
	}

	public static class StrategyBuilder<T> {
		protected final String key;
		protected final Storage storage;

		protected long ttlValue;
		protected TimeUnit ttlTimeUnit;
		protected String sessionName;
		protected Scheduler scheduler;

		protected CacheStrategy cacheStrategy = null;
		protected boolean keepExpiredCache = false;
		protected Single<T> asyncObservable = Single.never();

		public StrategyBuilder(@NotNull RxCache rxCache, @NotNull final String key, Object... args) {
			this.key = String.format(key, args);
			this.storage = rxCache.getStorage();
			this.ttlValue = rxCache.getDefaultTTLValue();
			this.ttlTimeUnit = rxCache.getDefaultTTLTimeUnit();
			this.sessionName = rxCache.getDefaultSessionName();
			this.scheduler = rxCache.getDefaultScheduler();
		}

		/**
		 * Apply the strategy to use
		 */
		public StrategyBuilder<T> withStrategy(@NotNull CacheStrategy cacheStrategy) {
			this.cacheStrategy = cacheStrategy;
			return this;
		}

		/**
		 * Apply the TTL (Time-To-Live) on the cached data. If data creation date exceeds this TTL, it will be considered expired
		 */
		public StrategyBuilder<T> withTTL(long value, @NotNull TimeUnit timeUnit) {
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
		 * Set the scheduler to use for cache observable
		 */
		public StrategyBuilder<T> withDefaultScheduler(@NotNull Scheduler scheduler) {
			this.scheduler = scheduler;
			return this;
		}

		/**
		 * The {@link Single} to use for async operations.
		 */
		public StrategyBuilder<T> withAsync(@Nullable Single<T> asyncObservable) {
			this.asyncObservable = asyncObservable == null ? Single.<T>never() : asyncObservable;
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
		 * Convert this resolution data strategy to a Rx {@link Flowable}
		 */
		public Flowable<T> fetch() {
			return fetchWrapper()
					.map(new Function<CacheWrapper<T>, T>() {
						@Override
						public T apply(@io.reactivex.annotations.NonNull CacheWrapper<T> cacheWrapper) throws Exception {
							return cacheWrapper.getData();
						}
					});
		}

		/**
		 * Convert this resolution data strategy to a Rx {@link Flowable}
		 */
		public Flowable<CacheWrapper<T>> fetchWrapper() {
			final Single<CacheWrapper<T>> asyncObservableCaching = buildAsyncObservableCaching(asyncObservable, sessionName, key);
			final Maybe<CacheWrapper<T>> cacheObservable = buildCacheObservable(sessionName, key);

			if (cacheStrategy == null) {
				cacheStrategy = CacheStrategy.cacheOrAsync(keepExpiredCache, ttlValue, ttlTimeUnit);
			}

			return cacheStrategy.getStrategyObservable(cacheObservable, asyncObservableCaching);
		}

		protected Single<CacheWrapper<T>> buildAsyncObservableCaching(@NotNull Single<T> asyncObservable, @Nullable final String sessionName, @NotNull final String key) {
			return asyncObservable
					.map(new Function<T, CacheWrapper<T>>() {
						@Override
						public CacheWrapper<T> apply(@io.reactivex.annotations.NonNull T value) throws Exception {
							return new CacheWrapper<>(value);
						}
					})
					.doOnSuccess(new Consumer<CacheWrapper<T>>() {
						@Override
						public void accept(@io.reactivex.annotations.NonNull CacheWrapper<T> value) throws Exception {
							storage.put(sessionName, key, value);
						}
					});
		}

		protected Maybe<CacheWrapper<T>> buildCacheObservable(@Nullable final String sessionName, @NotNull final String key) {
			return Maybe.fromCallable(new Callable<CacheWrapper<T>>() {
				@Override
				@SuppressWarnings("unchecked")
				public CacheWrapper<T> call() throws Exception {
					CacheWrapper<T> cachedData = (CacheWrapper<T>) storage.get(sessionName, key, Object.class);
					if (cachedData != null) {
						if (cachedData.getData() != null) {
							return cachedData.setFromCache(true);
						} else {
							storage.delete(sessionName, key);
						}
					}
					return null;
				}
			}).subscribeOn(scheduler);
		}
	}
}
