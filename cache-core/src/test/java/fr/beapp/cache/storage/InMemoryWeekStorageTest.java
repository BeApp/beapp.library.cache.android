package fr.beapp.cache.storage;

public class InMemoryWeekStorageTest extends BaseStorageTest {

	@Override
	protected Storage buildStorage() throws Exception {
		return new InMemoryStorage(true);
	}

}