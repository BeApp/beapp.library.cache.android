package fr.beapp.cache;


import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import fr.beapp.cache.strategy.CacheStrategy;

public class CacheStrategyTest {

	@Test
	public void testToString_asyncOrCache() throws Exception {
		Assert.assertEquals("ASYNC_OR_CACHE", CacheStrategy.asyncOrCache().toString());
	}

	@Test
	public void testToString_cacheOrAsync() throws Exception {
		Assert.assertEquals("CACHE_OR_ASYNC{keepExpiredCache=false, ttl=30 MINUTES}", CacheStrategy.cacheOrAsync().toString());
	}

	@Test
	public void testToString_cacheOrAsync_args() throws Exception {
		Assert.assertEquals("CACHE_OR_ASYNC{keepExpiredCache=true, ttl=60 SECONDS}", CacheStrategy.cacheOrAsync(true, 60, TimeUnit.SECONDS).toString());
	}

	@Test
	public void testToString_CacheThenAsync() throws Exception {
		Assert.assertEquals("CACHE_THEN_ASYNC", CacheStrategy.cacheThenAsync().toString());
	}

	@Test
	public void testToString_justCache() throws Exception {
		Assert.assertEquals("JUST_CACHE", CacheStrategy.justCache().toString());
	}

	@Test
	public void testToString_noCache() throws Exception {
		Assert.assertEquals("NO_CACHE", CacheStrategy.noCache().toString());
	}

}
