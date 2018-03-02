package fr.beapp.cache.storage;

import android.content.Context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

import fr.beapp.cache.CacheWrapper;
import io.paperdb.Book;
import io.paperdb.Paper;

/**
 * A {@link Storage} implementation based on <a href="https://github.com/pilgr/Paper">PaperDb</a>.
 * <br/>
 * This library relies on <a href="https://github.com/EsotericSoftware/kryo">Kryo</a> in order to provide fast serialization.
 */
public class PaperDbStorage implements Storage {

	private static final String DEFAULT_PAPER_BOOK = "io.paperdb";

	public PaperDbStorage(@NotNull Context context) {
		Paper.init(context);
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public int count() {
		Set<String> bookNames = PaperUtils.getAllPaperBookNames();
		return count(bookNames.toArray(new String[0]));
	}

	@Override
	public int count(@NotNull String[] sessions) {
		int count = 0;
		for (String session : sessions) {
			count += getBook(session).getAllKeys().size();
		}
		return count;
	}

	@Override
	public int count(@NotNull String session, @NotNull String keyPrefix) {
		List<String> allKeys = getBook(session).getAllKeys();

		int count = 0;
		for (String key : allKeys) {
			if (key.startsWith(keyPrefix)) {
				count++;
			}
		}
		return count;
	}

	@Override
	public synchronized void clear() {
		Set<String> bookNames = PaperUtils.getAllPaperBookNames();
		clear(bookNames.toArray(new String[0]));
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
			if (key.startsWith(keyPrefix)) {
				getBook(session).delete(key);
			}
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
		return session != null && !session.isEmpty() && !DEFAULT_PAPER_BOOK.equals(session) ? Paper.book(session) : Paper.book();
	}

}