package fr.beapp.cache.storage;

import android.content.Context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

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
	public synchronized void clear() {
		Paper.book().destroy();
	}

	@Override
	public synchronized void clear(@NotNull String keyPrefix) {
		List<String> keys = Paper.book().getAllKeys();
		for (String key : keys) {
			Paper.book().delete(key);
		}
	}

	@Override
	public synchronized void put(@NotNull String key, @Nullable Serializable value) {
		Paper.book().write(key, value);
	}

	@Override
	public synchronized void delete(@NotNull String key) {
		Paper.book().delete(key);
	}

	@Nullable
	@Override
	public synchronized <T extends Serializable> T get(@NotNull String key, @NotNull Class<T> clazz) {
		return Paper.book().read(key);
	}

	@NotNull
	@Override
	public synchronized <T extends Serializable> T get(@NotNull String key, @NotNull Class<T> clazz, @NotNull T defaultValue) {
		return Paper.book().read(key, defaultValue);
	}

	@Override
	public synchronized boolean exists(@NotNull String key) {
		return Paper.book().contains(key);
	}

}