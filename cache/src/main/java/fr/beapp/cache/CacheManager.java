package fr.beapp.cache;

import android.content.Context;
import android.support.annotation.NonNull;

import com.snappydb.DB;
import com.snappydb.SnappyDB;
import com.snappydb.SnappydbException;

import java.io.File;
import java.io.Serializable;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import fr.beapp.logger.Logger;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class CacheManager {

	private static CacheManager instance;

	protected final Context context;

	protected DB db;
	protected String sessionName;
	protected long ttlValue = 30;
	protected TimeUnit ttlTimeUnit = TimeUnit.MINUTES;

	public static CacheManager getInstance(Context context) {
		if (instance == null) {
			instance = new CacheManager(context.getApplicationContext());
		}
		return instance;
	}

	public CacheManager(Context context) {
		this.context = context;
	}

	private synchronized DB getDb() {
		return getDb(false);
	}

	public synchronized DB getDb(boolean wasForceDeleted) {
		if (db == null) {
			String path = context.getFilesDir().getAbsolutePath() + File.separator + "snappydb";
			String databaseName = "snappydb";

			try {
				Logger.info("Initializing SnappyDB database %s at %s", databaseName, path);

				db = new SnappyDB.Builder(context)
						.directory(path)
						.name(databaseName)
//						.registerSerializers(DateTime.class, new JodaDateTimeSerializer())
//						.registerSerializers(LocalDate.class, new JodaLocalDateSerializer())
//						.registerSerializers(PeriodType.class, new JodaPeriodTypeSerializer())
						.build();
			} catch (Exception e) {
				Logger.error("Can't open cache database. No data will be cached", e);

				// TODO Keep an eye on https://github.com/nhachicha/SnappyDB/issues/42 and implement a proper solution when available
				if (!wasForceDeleted) {    // Prevent deadly loop
					// For now, we force deleting the database if we received an Exception about corrupted database
					if (e.getLocalizedMessage().contains("Corruption")) {
						Logger.warn("SnappyDB database seems to be corrupted. Trying to delete it");

						File databaseFile = new File(path, databaseName);
						try {
							databaseFile.delete();
						} catch (Exception ignored) {
						}

						return getDb(true);
					}
				}
			}
		}
		return db;
	}

	public CacheManager withTTL(long value, @NonNull TimeUnit timeUnit) {
		this.ttlValue = value;
		this.ttlTimeUnit = timeUnit;
		return this;
	}

	public CacheManager withSession(@NonNull String sessionName) {
		this.sessionName = sessionName;
		return this;
	}

	public synchronized void closeDb() {
		if (db != null) {
			try {
				Logger.trace("Closing SnappyDB");

				db.close();
				db = null;
			} catch (SnappydbException e) {
				Logger.error("Can't close cache database", e);
			}
		}
	}

	public synchronized void clear() {
		try {
			getDb().destroy();
		} catch (SnappydbException e) {
			Logger.warn("Couldn't clear cache", e);
		}
	}

	public synchronized void put(String key, Serializable value) {
		String prependedKey = prependKey(key);

		try {
			if (!(value instanceof CacheWrapper)) {
				value = new CacheWrapper<>(value);
			}
			getDb().put(prependedKey, value);
		} catch (SnappydbException e) {
			Logger.warn("Data with key %s couldn't be put in cache", e, prependedKey);
		}
	}

	public synchronized void delete(String key) {
		String prependedKey = prependKey(key);

		try {
			getDb().del(prependedKey);
		} catch (SnappydbException e) {
			Logger.warn("Data with key %s couldn't be deleted from cache", e, prependedKey);
		}
	}

	public synchronized void deleteAll(String keyPrefix) {
		String prependedKey = prependKey(keyPrefix);

		try {
			String[] keys = getDb().findKeys(prependedKey);
			for (String key : keys) {
				getDb().del(key);
			}
		} catch (SnappydbException e) {
			Logger.warn("Couldn't clear keys with prefix %s", e, prependedKey);
		}
	}

	public synchronized <T extends Serializable> T get(String key, Class<T> clazz) {
		return get(key, clazz, null);
	}

	@SuppressWarnings("unchecked")
	public synchronized <T extends Serializable> T get(String key, Class<T> clazz, T defaultValue) {
		String prependedKey = prependKey(key);

		try {
			if (getDb().exists(prependedKey)) {
				CacheWrapper cacheWrapper = getDb().get(prependedKey, CacheWrapper.class);

				if (clazz.isAssignableFrom(CacheWrapper.class)) {
					return (T) cacheWrapper;
				} else if (cacheWrapper != null) {
					return (T) cacheWrapper.getData();
				}
			} else {
				return defaultValue;
			}
		} catch (SnappydbException e) {
			Logger.warn("Data with key %s couldn't be retrieved from cache. Deleting it", e, prependedKey);
			delete(prependedKey);
		}
		return defaultValue;
	}

	public synchronized boolean exists(String key) {
		String prependedKey = prependKey(key);

		try {
			return getDb().exists(prependedKey);
		} catch (SnappydbException e) {
			Logger.warn("Can't check if there is data for with key %s", e, prependedKey);
		}
		return false;
	}

	private String prependKey(String key) {
		return sessionName != null ? sessionName + key : key;
	}

	public String buildKey(String pattern, Object... args) {
		return String.format(Locale.ENGLISH, pattern, args);
	}

	/**
	 * Handle cache for the requested data. The asyncObservable should return a list of data if needed
	 */
	public <T> Observable<T> executeRx(final String key, final CacheStrategy strategy, final Observable<T> asyncObservable) {
		return loadData(key, strategy,
				asyncObservable.compose(new Observable.Transformer<T, T>() {
					@Override
					public Observable<T> call(Observable<T> observable) {
						return observable.doOnNext(new Action1<T>() {
							@Override
							public void call(T value) {
								if (value != null) {
									put(key, new CacheWrapper<>(value));
								}
							}
						});
					}
				})
		).subscribeOn(Schedulers.io());
	}

	private <T> Observable<T> loadData(final String key, final CacheStrategy strategy, final Observable<T> asyncObservable) {
		final Observable<CacheWrapper<T>> cacheObservable = Observable.create(new Observable.OnSubscribe<CacheWrapper<T>>() {
			@Override
			@SuppressWarnings("unchecked")
			public void call(Subscriber<? super CacheWrapper<T>> subscriber) {
				try {
					CacheWrapper<T> cachedData = get(key, CacheWrapper.class);
					if (cachedData != null) {
						subscriber.onNext(cachedData);
					}
					subscriber.onCompleted();
				} catch (Exception e) {
					subscriber.onError(e);
				}
			}
		});

		switch (strategy) {
			case CACHE_THEN_ASYNC:
				return cacheObservable
						.map(new Func1<CacheWrapper<T>, T>() {
							@Override
							public T call(CacheWrapper<T> cacheWrapper) {
								return cacheWrapper.getData();
							}
						})
						.concatWith(asyncObservable);
			case ASYNC_IF_NEEDED:
				return cacheObservable
						.filter(new Func1<CacheWrapper<T>, Boolean>() {
							@Override
							public Boolean call(CacheWrapper<T> cacheWrapper) {
								return !isExpired(cacheWrapper.getCachedDate());
							}
						})
						.map(new Func1<CacheWrapper<T>, T>() {
							@Override
							public T call(CacheWrapper<T> cacheWrapper) {
								return cacheWrapper.getData();
							}
						})
						.switchIfEmpty(asyncObservable
								.onErrorResumeNext(new Func1<Throwable, Observable<T>>() {
									@Override
									public Observable<T> call(Throwable throwable) {
										return cacheObservable
												.map(new Func1<CacheWrapper<T>, T>() {
													@Override
													public T call(CacheWrapper<T> cacheWrapper) {
														return cacheWrapper.getData();
													}
												})
												.switchIfEmpty(Observable.<T>error(throwable));
									}
								}));
			case JUST_CACHE:
				return cacheObservable
						.map(new Func1<CacheWrapper<T>, T>() {
							@Override
							public T call(CacheWrapper<T> cacheWrapper) {
								return cacheWrapper.getData();
							}
						});
			case NO_CACHE:
				return asyncObservable;
			case NO_CACHE_BUT_SAVED:
				return asyncObservable.onErrorResumeNext(new Func1<Throwable, Observable<T>>() {
					@Override
					public Observable<T> call(Throwable throwable) {
						return cacheObservable
								.map(new Func1<CacheWrapper<T>, T>() {
									@Override
									public T call(CacheWrapper<T> cacheWrapper) {
										return cacheWrapper.getData();
									}
								})
								.switchIfEmpty(Observable.<T>error(throwable));
					}
				});
		}
		return asyncObservable;
	}

	protected boolean isExpired(long cacheDate) {
		return System.currentTimeMillis() > cacheDate + TimeUnit.MILLISECONDS.convert(ttlValue, ttlTimeUnit);
	}
}
