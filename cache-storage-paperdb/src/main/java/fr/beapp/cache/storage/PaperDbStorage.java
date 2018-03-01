package fr.beapp.cache.storage;

import android.content.Context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import fr.beapp.cache.CacheWrapper;
import io.paperdb.Book;
import io.paperdb.Paper;

/**
 * A {@link Storage} implementation based on <a href="https://github.com/pilgr/Paper">PaperDb</a>.
 * <br/>
 * This library relies on <a href="https://github.com/EsotericSoftware/kryo">Kryo</a> in order to provide fast serialization.
 */
public class PaperDbStorage implements Storage {

	public PaperDbStorage(@NotNull Context context) {
		Paper.init(context);
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public synchronized void clear() {
		Paper.book().destroy();
	}

	@Override
	public synchronized void clear(@NotNull String... sessions) {
		for (String session : sessions) {
			getBook(session).destroy();
		}
	}

	@Override
	public synchronized void clear(@Nullable String session, @NotNull String keyPrefix) {
		List<String> keys = getBook(session).getAllKeys();
		for (String key : keys) {
			getBook(session).delete(key);
		}
	}

	@Override
	public synchronized <T> void put(@Nullable String session, @NotNull String key, @Nullable CacheWrapper<T> value) {
		getBook(session).write(key, value);
	}

	@Override
	public synchronized void delete(@Nullable String session, @NotNull String key) {
		getBook(session).delete(key);
	}

	@Nullable
	@Override
	public synchronized <T> CacheWrapper<T> get(@Nullable String session, @NotNull String key, @NotNull Class<T> clazz) {
		return getBook(session).read(key);
	}

	@NotNull
	@Override
	public synchronized <T> CacheWrapper<T> get(@Nullable String session, @NotNull String key, @NotNull Class<T> clazz, @NotNull T defaultValue) {
		return getBook(session).read(key, new CacheWrapper<>(defaultValue));
	}

	@Override
	public synchronized boolean exists(@Nullable String session, @NotNull String key) {
		return getBook(session).contains(key);
	}

	protected Book getBook(@Nullable String session) {
		return session != null && !session.isEmpty() ? Paper.book(session) : getBook(session);
	}

}