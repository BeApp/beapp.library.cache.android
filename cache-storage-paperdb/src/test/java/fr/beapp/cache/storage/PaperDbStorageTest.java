package fr.beapp.cache.storage;

import android.content.Context;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import androidx.test.core.app.ApplicationProvider;
import io.paperdb.Book;
import io.paperdb.Paper;

@RunWith(RobolectricTestRunner.class)
public class PaperDbStorageTest extends BaseStorageTest {

	@Override
	protected Storage buildStorage() throws Exception {
		Context context = ApplicationProvider.getApplicationContext();

		purgeFolder(context.getFilesDir());

		Field mBookMapField = Paper.class.getDeclaredField("mBookMap");
		mBookMapField.setAccessible(true);
		ConcurrentHashMap<String, Book> mBookMap = (ConcurrentHashMap<String, Book>) mBookMapField.get(null);
		mBookMap.clear();

		return new PaperDbStorage(context);
	}

	private void purgeFolder(File folder) {
		if (folder.isDirectory()) {
			File[] files = folder.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					purgeFolder(file);
				} else {
					file.delete();
				}
			}
			folder.delete();
		}
	}

}