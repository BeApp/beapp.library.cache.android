package fr.beapp.cache.storage;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.paperdb.Book;
import io.paperdb.Paper;

public final class PaperUtils {
	private PaperUtils() {
	}

	public static Set<String> getAllPaperBookNames() {
		try {
			Field mBookMapField = Paper.class.getDeclaredField("mBookMap");
			mBookMapField.setAccessible(true);
			ConcurrentHashMap<String, Book> mBookMap = (ConcurrentHashMap<String, Book>) mBookMapField.get(null);
			return mBookMap.keySet();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return Collections.emptySet();
	}
}
