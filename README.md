# Project
This project consists of two folders
- app
- simple-service

## `simple-service` folder
As the name suggest, this is a very simple application that runs a web server with 2 simple endpoint, as per described in the openapi.yaml file. 

## `app` folder
This is the application that works as the proxy to the simple-service. 

## Pre-requisites
- Jdk 17
- Docker

### How this works?
1. Go into `simple-service` folder
2. Run `./gradlew build`
3. Once completed, run `docker build -t simple-service .`
4. Run `docker images` and you should see `simple-service` in the list.
5. Now we run two containers of `simple-service` and listen to different port using docker.
  - `docker run --rm -p 8001:8000 simple-service`
  - `docker run --rm -p 8002:8000 simple-service`
6. Now, go to `app` folder and run `./gradlew build`
7. After finished building, go to `build/distributions` folder in app folder
8. Untar the application tar file by running `tar -xf app-1.0-SNAPSHOT.tar`
9. And run 
`./app-1.0-SNAPSHOT/bin/app instance[http://localhost:8001,http://localhost:8002]`
```the instance parameter consists the list of url to handle the requests, it needs to be a valid url```
10. The app will run at port 8000
11. We can use curl to send requests to the app and it will forward the request to the instances.
```
$ curl -v http://localhost:8000
*   Trying 127.0.0.1:8000...
* TCP_NODELAY set
* Connected to localhost (127.0.0.1) port 8000 (#0)
> GET / HTTP/1.1
> Host: localhost:8000
> User-Agent: curl/7.68.0
> Accept: */*
> 
* Mark bundle as not supporting multiuse
< HTTP/1.1 200 OK
< Date: Fri, 27 Jan 2023 09:46:01 GMT
< X-server: instance[http://localhost:8001]
< Content-length: 2
< 
* Connection #0 to host localhost left intact

$ curl -v http://localhost:8000
*   Trying 127.0.0.1:8000...
* TCP_NODELAY set
* Connected to localhost (127.0.0.1) port 8000 (#0)
> GET / HTTP/1.1
> Host: localhost:8000
> User-Agent: curl/7.68.0
> Accept: */*
> 
* Mark bundle as not supporting multiuse
< HTTP/1.1 200 OK
< Date: Fri, 27 Jan 2023 09:46:02 GMT
< X-server: instance[http://localhost:8002]
< Content-length: 2
< 
* Connection #0 to host localhost left intact
```


### The test cases
Both applications consists its own test cases. In `app` there are more test cases however, most of them are unit tests except `AppTest`, `AppTest` are more like an integration test where we will run docker containers using `simple-service` image and to perform the operation from end to end.