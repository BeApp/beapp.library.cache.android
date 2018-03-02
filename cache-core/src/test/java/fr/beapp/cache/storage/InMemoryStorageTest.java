package fr.beapp.cache.storage;

public class InMemoryStorageTest extends BaseStorageTest {

	@Override
	protected Storage buildStorage() throws Exception {
		return new InMemoryStorage(false);
	}

}