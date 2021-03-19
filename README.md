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
    <version>1.0.2</version>
</dependency>
```



### How to use it? ##

Getting an auto-refreshing lock with default configuration
```
RedUtilsLock redUtilsLock = new RedUtilsLockImpl();
redUtilsLock.acquire("lock1", () -> {
    // do some operation
});
```

