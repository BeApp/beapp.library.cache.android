package fr.beapp.cache;

import org.junit.Before;
import org.junit.Test;

import fr.beapp.cache.internal.CacheWrapper;
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
		rxCache.executeRx("key", CacheStrategy.CACHE_THEN_ASYNC, Observable.just(ASYNC_OBJECT)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_cacheThenAsync_noCache_asyncError() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.CACHE_THEN_ASYNC, Observable.error(asyncException)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertError(asyncException);
		testSubscriber.assertNoValues();
	}

	@Test
	public void testExecuteRx_cacheThenAsync_cachedValue_asyncOk() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.CACHE_THEN_ASYNC, Observable.just(ASYNC_OBJECT)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(2);
		testSubscriber.assertValues(CACHE_OBJECT, ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_cacheThenAsync_cachedValue_asyncError() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.CACHE_THEN_ASYNC, Observable.error(asyncException)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertError(asyncException);
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValues(CACHE_OBJECT);
	}

	// ASYNC_IF_NEEDED

	@Test
	public void testExecuteRx_asyncIfNeeded_noCache_asyncOk() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.ASYNC_IF_NEEDED, Observable.just(ASYNC_OBJECT)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_asyncIfNeeded_noCache_asyncError() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.ASYNC_IF_NEEDED, Observable.error(asyncException)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertError(asyncException);
		testSubscriber.assertNoValues();
	}

	@Test
	public void testExecuteRx_asyncIfNeeded_expiredCache_asyncOk() throws Exception {
		storage.put("key", new CacheWrapper<>(System.currentTimeMillis() - MINUTES_60, CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.ASYNC_IF_NEEDED, Observable.just(ASYNC_OBJECT)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_asyncIfNeeded_expiredCache_asyncError() throws Exception {
		storage.put("key", new CacheWrapper<>(System.currentTimeMillis() - MINUTES_60, CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.ASYNC_IF_NEEDED, Observable.error(asyncException)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();    // TODO Async in error but we have expired value from cache. Should we really swallow this exception ?
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValues(CACHE_OBJECT);
	}

	@Test
	public void testExecuteRx_asyncIfNeeded_cachedValue_asyncOk() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.ASYNC_IF_NEEDED, Observable.just(ASYNC_OBJECT)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValues(CACHE_OBJECT);
	}

	@Test
	public void testExecuteRx_asyncIfNeeded_cachedValue_asyncError() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.ASYNC_IF_NEEDED, Observable.error(asyncException)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();    // TODO Should we really swallow this exception ?
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValues(CACHE_OBJECT);
	}

	// JUST_CACHE

	@Test
	public void testExecuteRx_justCache_noCache_asyncOk() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.JUST_CACHE, Observable.just(ASYNC_OBJECT)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertNoValues();
	}

	@Test
	public void testExecuteRx_justCache_noCache_asyncError() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.JUST_CACHE, Observable.error(asyncException)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();    // Ok, because Async shouldn't be called with this strategy
		testSubscriber.assertNoValues();
	}

	@Test
	public void testExecuteRx_justCache_cachedValue_asyncOk() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.JUST_CACHE, Observable.just(ASYNC_OBJECT)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(CACHE_OBJECT);
	}

	@Test
	public void testExecuteRx_justCache_cachedValue_asyncError() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.JUST_CACHE, Observable.just(ASYNC_OBJECT)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();    // Ok, because Async shouldn't be called with this strategy
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(CACHE_OBJECT);
	}

	// NO_CACHE

	@Test
	public void testExecuteRx_noCache_noCache_asyncOk() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.NO_CACHE, Observable.just(ASYNC_OBJECT)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_noCache_noCache_asyncError() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.NO_CACHE, Observable.error(asyncException)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertError(asyncException);
		testSubscriber.assertNoValues();
	}

	@Test
	public void testExecuteRx_noCache_cachedValue_asyncOk() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.NO_CACHE, Observable.just(ASYNC_OBJECT)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_noCache_cachedValue_asyncError() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.NO_CACHE, Observable.error(asyncException)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertError(asyncException);
		testSubscriber.assertNoValues();
	}

	// NO_CACHE_BUT_SAVED

	@Test
	public void testExecuteRx_noCacheButSaved_noCache_asyncOk() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.NO_CACHE_BUT_SAVED, Observable.just(ASYNC_OBJECT)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_noCacheButSaved_noCache_asyncError() throws Exception {
		storage.put("otherKey", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.NO_CACHE_BUT_SAVED, Observable.error(asyncException)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertError(asyncException);
		testSubscriber.assertNoValues();
	}

	@Test
	public void testExecuteRx_noCacheButSaved_cachedValue_asyncOk() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.NO_CACHE_BUT_SAVED, Observable.just(ASYNC_OBJECT)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(ASYNC_OBJECT);
	}

	@Test
	public void testExecuteRx_noCacheButSaved_cachedValue_asyncError() throws Exception {
		storage.put("key", new CacheWrapper<>(CACHE_OBJECT));
		rxCache.executeRx("key", CacheStrategy.NO_CACHE_BUT_SAVED, Observable.error(asyncException)).subscribe(testSubscriber);

		testSubscriber.awaitTerminalEvent();
		testSubscriber.assertNoErrors();    // TODO Async in error but we have value from cache. Should we really swallow this exception ?
		testSubscriber.assertValueCount(1);
		testSubscriber.assertValue(CACHE_OBJECT);
	}


}