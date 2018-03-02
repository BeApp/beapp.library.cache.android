package fr.beapp.cache.storage;

import org.junit.Test;

import fr.beapp.cache.CacheWrapper;

import static org.junit.Assert.assertEquals;

public abstract class BaseStorageTest {

	protected abstract Storage buildStorage() throws Exception;

	@Test
	public void testPutGet() throws Exception {
		long cachedDate = System.currentTimeMillis();
		Storage storage = getStorage(cachedDate);

		assertEquals(null, storage.get(null, "", String.class));
		assertEquals(null, storage.get(null, "unknownKey", String.class));
		assertEquals(null, storage.get(null, "sessionKey", String.class));
		assertEquals(null, storage.get("unknownSession", "", String.class));
		assertEquals(null, storage.get("unknownSession", "unknownKey", String.class));
		assertEquals(null, storage.get("unknownSession", "sessionKey", String.class));
		assertEquals(null, storage.get("session1", "", String.class));
		assertEquals(null, storage.get("session1", "unknownKey", String.class));
		assertEquals(null, storage.get("session1", "globalKey", String.class));

		assertEquals(new CacheWrapper<>(cachedDate, "globalValue"), storage.get(null, "globalKey", String.class));
		assertEquals(new CacheWrapper<>(cachedDate, "globalValue1"), storage.get(null, "key1", String.class));
		assertEquals(new CacheWrapper<>(cachedDate, "globalValue2"), storage.get(null, "key2", String.class));
		assertEquals(new CacheWrapper<>(cachedDate, "session1Value"), storage.get("session1", "sessionKey", String.class));
		assertEquals(new CacheWrapper<>(cachedDate, "session1Value1"), storage.get("session1", "key1", String.class));
		assertEquals(new CacheWrapper<>(cachedDate, "session1Value2"), storage.get("session1", "key2", String.class));
	}

	@Test
	public void testCount() throws Exception {
		long cachedDate = System.currentTimeMillis();
		Storage storage = getStorage(cachedDate);

		assertEquals(6, storage.count());

		assertEquals(3, storage.count(""));
		assertEquals(0, storage.count("unknownSession"));
		assertEquals(3, storage.count(new String[]{"unknownSession", "session1"}));

		assertEquals(3, storage.count("", ""));
		assertEquals(0, storage.count("unknownSession", "unknownKey"));
		assertEquals(0, storage.count("unknownSession", "key1"));
		assertEquals(3, storage.count("session1", ""));
		assertEquals(2, storage.count("session1", "ke"));
		assertEquals(0, storage.count("session1", "unknownKey"));
	}

	@Test
	public void testClear() throws Exception {
		long cachedDate = System.currentTimeMillis();
		Storage storage = getStorage(cachedDate);

		storage.clear();
		assertEquals(0, storage.count());
	}

	@Test
	public void testClear_withSession() throws Exception {
		long cachedDate = System.currentTimeMillis();
		Storage storage = getStorage(cachedDate);

		storage.clear(new String[]{"unknownSession", "session1"});
		assertEquals(3, storage.count());
	}

	@Test
	public void testClear_withSessionAndKey() throws Exception {
		long cachedDate = System.currentTimeMillis();
		Storage storage = getStorage(cachedDate);

		storage.clear(null, "ke");
		storage.clear("session1", "ke");
		assertEquals(2, storage.count());
	}

	private Storage getStorage(long cachedDate) throws Exception {
		Storage storage = buildStorage();
		storage.put(null, "globalKey", new CacheWrapper<>(cachedDate, "globalValue"));
		storage.put(null, "key1", new CacheWrapper<>(cachedDate, "globalValue1"));
		storage.put(null, "key2", new CacheWrapper<>(cachedDate, "globalValue2"));
		storage.put("session1", "sessionKey", new CacheWrapper<>(cachedDate, "session1Value"));
		storage.put("session1", "key1", new CacheWrapper<>(cachedDate, "session1Value1"));
		storage.put("session1", "key2", new CacheWrapper<>(cachedDate, "session1Value2"));
		return storage;
	}
}