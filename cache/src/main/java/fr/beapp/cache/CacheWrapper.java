package fr.beapp.cache;

import java.io.Serializable;

public class CacheWrapper<T> implements Serializable {

	private boolean fromCache = false;
	private long cachedDate;
	private T data;

	public CacheWrapper() {
		// Needed for deserialization
	}

	public CacheWrapper(T data) {
		this(System.currentTimeMillis(), data);
	}

	public CacheWrapper(long cachedDate, T data) {
		this.cachedDate = cachedDate;
		this.data = data;
	}

	@Override
	public String toString() {
		return "CacheWrapper{" +
				"fromCache=" + fromCache +
				", cachedDate=" + cachedDate +
				", data=" + data +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		CacheWrapper<?> that = (CacheWrapper<?>) o;

		if (cachedDate != that.cachedDate) return false;
		return !(data != null ? !data.equals(that.data) : that.data != null);

	}

	@Override
	public int hashCode() {
		int result = (int) (cachedDate ^ (cachedDate >>> 32));
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	public boolean isFromCache() {
		return fromCache;
	}

	public CacheWrapper<T> setFromCache(boolean fromCache) {
		this.fromCache = fromCache;
		return this;
	}

	public long getCachedDate() {
		return cachedDate;
	}

	public void setCachedDate(long cachedDate) {
		this.cachedDate = cachedDate;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}
}
