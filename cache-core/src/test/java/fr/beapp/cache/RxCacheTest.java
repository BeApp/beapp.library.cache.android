package fr.beapp.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import fr.beapp.cache.storage.InMemoryStorage;
import fr.beapp.cache.storage.Storage;
import fr.beapp.cache.strategy.CacheStrategy;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class RxCacheTest {

	private static final Object ASYNC_OBJECT = new Object() {
		@Override
		public String toString() {
			return "Async Object";
		}
	};
	private static final Object CACHE_OBJECT = new Object() {
		@Override
		public String toString() {
			return "Cache Object";
		}
	};
	@SuppressWarnings("ThrowableInstanceNeverThrown")
	private static Exception asyncException = new Exception() {
	};

	private static final Scheduler OBSERVE_ON_SCHEDULER = Schedulers.trampoline();
	private static final int MINUTES_60 = 3600 * 1000;

	private final Storage storage = new InMemoryStorage(false) {
		@Nullable
		@Override
		public <T> CacheWrapper<T> get(@Nullable String session, @NotNull String key, @NotNull Class<T> clazz) {
			// Simulate a little latency
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return super.get(session, key, clazz);
		}
	};
	private final RxCache rxCache = new RxCache(storage);
	private final String session = null;
	private TestSubscriber<Object> testObserver;

	@Before
	public void initTest() {
		storage.clear();
		testObserver = TestSubscriber.create();
	}

	private Single<Object> async() {
		return Single.just(ASYNC_OBJECT)
				.delay(50, TimeUnit.MILLISECONDS);
	}

	// CACHE_THEN_ASYNC

	@Test
	public void testExecuteRx_cacheThenAsync_noCache_asyncOk() throws Exception {
		storage.put(session, "otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheThenAsync())
				.withAsync(async())
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertValueCount(1);
		testObserver.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_cacheThenAsync_noCache_asyncError() throws Exception {
		storage.put(session, "otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheThenAsync())
				.withAsync(Single.error(asyncException))
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertNotComplete();
		testObserver.assertError(asyncException);
		testObserver.assertNoValues();
	}

	@Test
	public void testExecuteRx_cacheThenAsync_cachedValue_asyncOk() throws Exception {
		storage.put(session, "key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheThenAsync())
				.withAsync(async())
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertValueCount(2);
		testObserver.assertValues(CACHE_OBJECT, ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_cacheThenAsync_cachedValue_asyncError() throws Exception {
		storage.put(session, "key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheThenAsync())
				.withAsync(Single.error(asyncException))
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertNotComplete();
		testObserver.assertError(asyncException);
		testObserver.assertValueCount(1);
		testObserver.assertValues(CACHE_OBJECT);
	}

	// cacheOrAsync()

	@Test
	public void testExecuteRx_cacheOrAsync_noCache_asyncOk() throws Exception {
		storage.put(session, "otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheOrAsync())
				.withAsync(async())
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertValueCount(1);
		testObserver.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_cacheOrAsync_noCache_asyncError() throws Exception {
		storage.put(session, "otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheOrAsync())
				.withAsync(Single.error(asyncException))
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertNotComplete();
		testObserver.assertError(asyncException);
		testObserver.assertNoValues();
	}

	@Test
	public void testExecuteRx_cacheOrAsync_expiredCache_asyncOk() throws Exception {
		storage.put(session, "key", new CacheWrapper<>(System.currentTimeMillis() - MINUTES_60, CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheOrAsync())
				.withAsync(async())
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertValueCount(1);
		testObserver.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_cacheOrAsync_expiredCache_asyncError() throws Exception {
		storage.put(session, "key", new CacheWrapper<>(System.currentTimeMillis() - MINUTES_60, CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheOrAsync())
				.withAsync(Single.error(asyncException))
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();    // TODO Async in error but we have expired value from cache. Should we really swallow this exception ?
		testObserver.assertValueCount(1);
		testObserver.assertValues(CACHE_OBJECT);
	}

	@Test
	public void testExecuteRx_cacheOrAsync_cachedValue_asyncOk() throws Exception {
		storage.put(session, "key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheOrAsync())
				.withAsync(async())
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertValueCount(1);
		testObserver.assertValues(CACHE_OBJECT);
	}

	@Test
	public void testExecuteRx_cacheOrAsync_cachedValue_asyncError() throws Exception {
		storage.put(session, "key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheOrAsync())
				.withAsync(Single.error(asyncException))
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();    // TODO Should we really swallow this exception ?
		testObserver.assertValueCount(1);
		testObserver.assertValues(CACHE_OBJECT);
	}

	// justCache()

	@Test
	public void testExecuteRx_justCache_noCache_asyncOk() throws Exception {
		storage.put(session, "otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.justCache())
				.withAsync(async())
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertNoValues();
	}

	@Test
	public void testExecuteRx_justCache_noCache_asyncError() throws Exception {
		storage.put(session, "otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.justCache())
				.withAsync(Single.error(asyncException))
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();    // Ok, because Async shouldn't be called with this strategy
		testObserver.assertNoValues();
	}

	@Test
	public void testExecuteRx_justCache_cachedValue_asyncOk() throws Exception {
		storage.put(session, "key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.justCache())
				.withAsync(async())
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertValueCount(1);
		testObserver.assertValue(CACHE_OBJECT);
	}

	@Test
	public void testExecuteRx_justCache_cachedValue_asyncError() throws Exception {
		storage.put(session, "key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.justCache())
				.withAsync(async())
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();    // Ok, because Async shouldn't be called with this strategy
		testObserver.assertValueCount(1);
		testObserver.assertValue(CACHE_OBJECT);
	}

	// noCache()

	@Test
	public void testExecuteRx_noCache_noCache_asyncOk() throws Exception {
		storage.put(session, "otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.noCache())
				.withAsync(async())
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertValueCount(1);
		testObserver.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_noCache_noCache_asyncError() throws Exception {
		storage.put(session, "otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.noCache())
				.withAsync(Single.error(asyncException))
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertNotComplete();
		testObserver.assertError(asyncException);
		testObserver.assertNoValues();
	}

	@Test
	public void testExecuteRx_noCache_cachedValue_asyncOk() throws Exception {
		storage.put(session, "key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.noCache())
				.withAsync(async())
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertValueCount(1);
		testObserver.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_noCache_cachedValue_asyncError() throws Exception {
		storage.put(session, "key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.noCache())
				.withAsync(Single.error(asyncException))
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertNotComplete();
		testObserver.assertError(asyncException);
		testObserver.assertNoValues();
	}

	// asyncOrCache()

	@Test
	public void testExecuteRx_asyncOrCache_noCache_asyncOk() throws Exception {
		storage.put(session, "otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.asyncOrCache())
				.withAsync(async())
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertValueCount(1);
		testObserver.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_asyncOrCache_noCache_asyncError() throws Exception {
		storage.put(session, "otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.asyncOrCache())
				.withAsync(Single.error(asyncException))
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertNotComplete();
		testObserver.assertError(asyncException);
		testObserver.assertNoValues();
	}

	@Test
	public void testExecuteRx_asyncOrCache_cachedValue_asyncOk() throws Exception {
		storage.put(session, "key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.asyncOrCache())
				.withAsync(async())
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertValueCount(1);
		testObserver.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_asyncOrCache_cachedValue_asyncError() throws Exception {
		storage.put(session, "key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.asyncOrCache())
				.withAsync(Single.error(asyncException))
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();    // TODO Async in error but we have value from cache. Should we really swallow this exception ?
		testObserver.assertValueCount(1);
		testObserver.assertValue(CACHE_OBJECT);
	}

	// custom

	@Test
	public void testExecuteRx_custom_noCache_asyncOk() throws Exception {
		final Object customObject = new Object() {
			@Override
			public String toString() {
				return "Custom Object";
			}
		};

		storage.put(session, "otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(new CacheStrategy("CUSTOM_STRATEGY") {
					@Override
					public <T> Flowable<CacheWrapper<T>> getStrategyObservable(@NotNull Maybe<CacheWrapper<T>> cacheObservable, @NotNull Single<CacheWrapper<T>> asyncObservable) {
						return Flowable.just(new CacheWrapper<>((T) customObject));
					}
				})
				.withAsync(async())
				.fetch()
				.observeOn(OBSERVE_ON_SCHEDULER)
				.subscribe(testObserver);

		testObserver.awaitTerminalEvent();
		testObserver.assertComplete();
		testObserver.assertNoErrors();
		testObserver.assertValueCount(1);
		testObserver.assertValue(customObject);
	}

}