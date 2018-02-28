package fr.beapp.cachesample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import fr.beapp.cache.CacheWrapper;
import fr.beapp.cache.RxCache;
import fr.beapp.cache.storage.SnappyDBStorage;
import fr.beapp.cache.strategy.CacheStrategy;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final SnappyDBStorage snappyDBStorage = new SnappyDBStorage(this);
		final RxCache rxCache = new RxCache(snappyDBStorage);

		final TextView resultText = findViewById(R.id.resultText);

		Button loadButton = findViewById(R.id.loadButton);
		loadButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				rxCache.<Date>fromKey("test")
						.withStrategy(CacheStrategy.cacheOrAsync())
						.withAsync(Single.just(new Date()))
						.fetchWrapper()
						.observeOn(AndroidSchedulers.mainThread())
						.subscribe(new Consumer<CacheWrapper<Date>>() {
							@Override
							public void accept(CacheWrapper<Date> wrapper) throws Exception {
								resultText.setText(String.format("From cache: %b\nDate: %s", wrapper.isFromCache(), SimpleDateFormat.getDateTimeInstance().format(wrapper.getData())));
							}
						});
			}
		});
	}
}
