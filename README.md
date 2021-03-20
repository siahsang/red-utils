<h1 align="center"> 
    RedUtils
</h1>  

<h4 align="center">Distributed Lock Implementation With Redis</h4>

<p align="center">
    <a href="http://www.apache.org/licenses/LICENSE-2.0">
        <img src="https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat" alt="license" title="">
    </a>
    <a href="https://travis-ci.com/github/siahsang/red-utils">
        <img src="https://travis-ci.com/siahsang/red-utils.svg?token=N599nN4MvyuvHP5RhDbq&branch=develop" alt="Build Status">
    </a>
    <a href="https://codecov.io/gh/siahsang/red-utils">
        <img src="https://codecov.io/gh/siahsang/red-utils/branch/develop/graph/badge.svg?token=9OF1191T9L"/>
    </a>

</p>


## Introduction ##
RedUtils is a distributed lock and using Redis for storing and expiring locks with. It has the following feature:

-  **Leased-Based Lock**: If any clients crash or restarted abnormally, eventually lock will be free. 
-  **Safe**: Provided that fsync=always on every Redis instance we have safety even if Redis become unavailable after getting lock. 
-  **Auto-Refreshing Lock**: A lock that is acquired by the client can be held as long as the client is alive, and the connection is OK. 


## Getting Started ##

### Requirements ##
Install Redis or using following command if you have Docker installed
```
docker run --name some-redis  -e ALLOW_EMPTY_PASSWORD=yes -p 6379:6379 --rm -it redis
```

Add the following dependency (Java 8 is required)

```
<dependency>
    <groupId>com.github.siahsang</groupId>
    <artifactId>red-utils</artifactId>
    <version>1.0.4</version>
</dependency>
```



### How to use it? ##

Getting the lock with the default configuration. Wait for getting lock if it is acquired by another thread.

```
RedUtilsLock redUtilsLock = new RedUtilsLockImpl();
redUtilsLock.acquire("lock1", () -> {
    // some operation
});
```

Try to acquire the lock and return true after executing the operation, otherwise, return false immediately.
```
RedUtilsLock redUtilsLock = new RedUtilsLockImpl();
boolean getLockSuccessfully = redUtilsLock.tryAcquire("lock1", () -> {
    // some operation
});
```

You can also provide configuration when initializing RedUtilsLock
```
RedUtilsConfig redUtilsConfig = new RedUtilsConfig.RedUtilsConfigBuilder()
            .hostAddress("127.0.0.1")
            .port("6379")
            .replicaCount(3)
            .leaseTimeMillis(40_000)
            .build();

RedUtilsLock redUtilsLock = new RedUtilsLockImpl(redUtilsConfig);
```

To see more example please see tests


### Running the tests ###
For running the tests, you should install Docker(test cases use [testcontainer](https://www.testcontainers.org/) for running Redis). 
After that you can run all tests with:
``` 
mvn clean test
```

## Caveats ##
There are some caveats that you should be aware of:

1. I assume clocks are synchronized between different nodes
2. I assume there aren't any long thread pause or process pause after getting lock but before using it
3. To achieve strong consistency you should enable the option fsync=always on every Redis instance  
4. In current implementation locks is not fair; for example, a client may wait a long time to get the lock and at the same time another client get the lock immediately

