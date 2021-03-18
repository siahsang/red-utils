<h1 align="center"> 
    RedUtils
</h1>  

<h4 align="center">Distributed Lock Implementation With Redis</h4>

<p align="center">
    <a href="http://www.apache.org/licenses/LICENSE-2.0"><img src="https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat" alt="license" title=""></a>
    <a href="https://travis-ci.com/github/siahsang/red-utils"><img src="https://travis-ci.com/siahsang/red-utils.svg?token=N599nN4MvyuvHP5RhDbq&branch=develop" alt="Build Status"></a>
</p>


## Introduction ##
RedUtils is a distributed lock and using Redis for storing and expiring locks with. It has the following feature:

-  **Leased-Based Lock** : If any clients crash or restarted abnormally, eventually lock will be free. 
-  **Safe** : Provided that fsync=always on every Redis instance we have safety even if Redis become unavailable after getting lock. 
-  **Auto-Refreshing Lock** : A lock that is acquired by the client can be held as long as the client is alive, and the connection is OK. 


## Getting Started ##
First add the following dependency (Java 8 is required)


