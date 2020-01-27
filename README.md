This library provides a cache mechanism relying on [RxJava](https://github.com/ReactiveX/RxJava).
There are currently two storage implementation :

* [SnappyDb](https://github.com/nhachicha/SnappyDB)
* [PaperDb](https://github.com/pilgr/Paper)

# Usage

Two steps are needed :

1. Prepare an instance of your storage implementation
2. Declare an instance of RxCache with the storage implementation
3. Call `fromKey` method from RxCache instance and configure the strategy for this call
 
 
```java
final Storage storage = new SnappyDBStorage();
final RxCache rxCache = new RxCache(storage);

rxCache.fromKey("remotedata_%d", 1)
    .withStrategy(CacheStrategy.cacheThenAsync())
    .withAsync(...) // Your async observable method (Retrofit call for example)
    .toObservable()
    .subscribe(result -> ..., 
        throwable -> ...);
```

# Installation

Add jcenter's repository in your project's repositories list, then add the dependency.

```groovy
repositories {
    jcenter()
}

dependencies {
    implementation 'fr.beapp.cache:cache-core:<latest-version>'

    // Pick one of the following
    implementation 'fr.beapp.cache:cache-storage-snappydb:<latest-version>'
    implementation 'fr.beapp.cache:cache-storage-paperdb:<latest-version>'
}
```