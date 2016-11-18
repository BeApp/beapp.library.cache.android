This library provides a cache mechanism relying on [RxJava](https://github.com/ReactiveX/RxJava) and [SnappyDB](https://github.com/nhachicha/SnappyDB) as default storage implementation.

# Usage

Two steps are needed :

1. Declare an instance of RxCache with a storage implementation (default is SnappyDB)
2. Call `fromKey` method from RxCache instance and configure the strategy for this call
 
 
```java
final InMemoryStorage storage = new InMemoryStorage();
final RxCache rxCache = new RxCache(storage);

rxCache.fromKey("remotedata_%d", 1)
    .withStrategy(CacheStrategy.cacheThenAsync())
    .withAsync(...) // Your async observable method (Retrofit call for example)
    .toObservable()
    .subscribe(result -> ..., 
        throwable -> ...);
```

# Installation

Add Beapp's repository in your project's repositories list, then add the dependency.

```groovy
repositories {
    jcenter()
    // ...
    maven { url 'http://repository.beapp.fr/libs-release-local' }
}

dependencies {
    compile 'fr.beapp.cache:cache:<version>'
}
```