package fr.beapp.cache;

import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import fr.beapp.cache.internal.CacheWrapper;
import fr.beapp.cache.strategy.CacheStrategy;
import rx.Observable;
import rx.observers.TestSubscriber;

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

	private static final int MINUTES_60 = 3600 * 1000;

	private final InMemoryStorage storage = new InMemoryStorage();
	private final RxCache rxCache = new RxCache(storage);
	private TestSubscriber<Object> testSubscriber;

	@Before
	public void initTest() {
		storage.clear();
		testSubscriber = new TestSubscriber<>();
	}

	// CACHE_THEN_ASYNC

	@Test
	public void testExecuteRx_cacheThenAsync_noCache_asyncOk() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheThenAsync())
				.withAsync(Observable.just(ASYNC_OBJECT))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_cacheThenAsync_noCache_asyncError() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheThenAsync())
				.withAsync(Observable.error(asyncException))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertError(asyncException);
		testSubscriber.assertNoValues();
	}

	@Test
	public void testExecuteRx_cacheThenAsync_cachedValue_asyncOk() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheThenAsync())
				.withAsync(Observable.just(ASYNC_OBJECT))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(2);
		testSubscriber.assertValues(CACHE_OBJECT, ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_cacheThenAsync_cachedValue_asyncError() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheThenAsync())
				.withAsync(Observable.error(asyncException))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertError(asyncException);
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValues(CACHE_OBJECT);
	}

	// cacheOrAsync()

	@Test
	public void testExecuteRx_cacheOrAsync_noCache_asyncOk() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheOrAsync())
				.withAsync(Observable.just(ASYNC_OBJECT))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_cacheOrAsync_noCache_asyncError() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheOrAsync())
				.withAsync(Observable.error(asyncException))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertError(asyncException);
		testSubscriber.assertNoValues();
	}

	@Test
	public void testExecuteRx_cacheOrAsync_expiredCache_asyncOk() throws Exception {
		storage.put("key", new CacheWrapper<>(System.currentTimeMillis() - MINUTES_60, CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheOrAsync())
				.withAsync(Observable.just(ASYNC_OBJECT))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_cacheOrAsync_expiredCache_asyncError() throws Exception {
		storage.put("key", new CacheWrapper<>(System.currentTimeMillis() - MINUTES_60, CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheOrAsync())
				.withAsync(Observable.error(asyncException))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();    // TODO Async in error but we have expired value from cache. Should we really swallow this exception ?
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValues(CACHE_OBJECT);
	}

	@Test
	public void testExecuteRx_cacheOrAsync_cachedValue_asyncOk() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheOrAsync())
				.withAsync(Observable.just(ASYNC_OBJECT))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValues(CACHE_OBJECT);
	}

	@Test
	public void testExecuteRx_cacheOrAsync_cachedValue_asyncError() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.cacheOrAsync())
				.withAsync(Observable.error(asyncException))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();    // TODO Should we really swallow this exception ?
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValues(CACHE_OBJECT);
	}

	// justCache()

	@Test
	public void testExecuteRx_justCache_noCache_asyncOk() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.justCache())
				.withAsync(Observable.just(ASYNC_OBJECT))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertNoValues();
	}

	@Test
	public void testExecuteRx_justCache_noCache_asyncError() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.justCache())
				.withAsync(Observable.error(asyncException))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();    // Ok, because Async shouldn't be called with this strategy
		testSubscriber.assertNoValues();
	}

	@Test
	public void testExecuteRx_justCache_cachedValue_asyncOk() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.justCache())
				.withAsync(Observable.just(ASYNC_OBJECT))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(CACHE_OBJECT);
	}

	@Test
	public void testExecuteRx_justCache_cachedValue_asyncError() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.justCache())
				.withAsync(Observable.just(ASYNC_OBJECT))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();    // Ok, because Async shouldn't be called with this strategy
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(CACHE_OBJECT);
	}

	// noCache()

	@Test
	public void testExecuteRx_noCache_noCache_asyncOk() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.noCache())
				.withAsync(Observable.just(ASYNC_OBJECT))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_noCache_noCache_asyncError() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.noCache())
				.withAsync(Observable.error(asyncException))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertError(asyncException);
		testSubscriber.assertNoValues();
	}

	@Test
	public void testExecuteRx_noCache_cachedValue_asyncOk() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.noCache())
				.withAsync(Observable.just(ASYNC_OBJECT))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_noCache_cachedValue_asyncError() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.noCache())
				.withAsync(Observable.error(asyncException))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertError(asyncException);
		testSubscriber.assertNoValues();
	}

	// asyncOrCache()

	@Test
	public void testExecuteRx_asyncOrCache_noCache_asyncOk() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.asyncOrCache())
				.withAsync(Observable.just(ASYNC_OBJECT))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_asyncOrCache_noCache_asyncError() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.asyncOrCache())
				.withAsync(Observable.error(asyncException))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertError(asyncException);
		testSubscriber.assertNoValues();
	}

	@Test
	public void testExecuteRx_asyncOrCache_cachedValue_asyncOk() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.asyncOrCache())
				.withAsync(Observable.just(ASYNC_OBJECT))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_asyncOrCache_cachedValue_asyncError() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(CacheStrategy.asyncOrCache())
				.withAsync(Observable.error(asyncException))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();    // TODO Async in error but we have value from cache. Should we really swallow this exception ?
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(CACHE_OBJECT);
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

		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.fromKey("key")
				.withStrategy(new CacheStrategy() {
					@Override
					@SuppressWarnings("unchecked")
					public <T> Observable<CacheWrapper<T>> getStrategyObservable(@NonNull Observable<CacheWrapper<T>> cacheObservable, @NonNull Observable<CacheWrapper<T>> asyncObservable) {
						return Observable.just(new CacheWrapper<>((T) customObject));
					}
				})
				.withAsync(Observable.just(ASYNC_OBJECT))
				.toObservable().subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(customObject);
	}

}