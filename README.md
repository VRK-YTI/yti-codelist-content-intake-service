# YTI CodeList - Content Intake Service microservice  

This application is part of the [Joint metadata and information management programme](https://wiki.julkict.fi/julkict/yti).

## Description

This is the implementation of the Content Intake Service microservice for the YTI CodeList (yti-codelist) with:

* [Spring boot] For getting things up and running
* Embedded [Jetty] to serve
* [Jersey 2] for JAX-RS

## Interface Documentation

When the microservice is running, you can get the Swagger REST API documentation from:
- [http://localhost:9602/api/swagger.json](http://localhost:9602/api/swagger.json)
- [http://localhost:9602/swagger/index.html](http://localhost:9602/swagger/index.html)

## Prerequisities

### Building
- Java 8+
- Maven 3.3+
- Docker

## Running

- [yti-codelist-config](https://github.com/vrk-yti/yti-codelist-config/) - Default configuration for development use.

## Starting service on local development environment

### Running inside IDE

Add the following Run configurations options:

- Program arguments: `--spring.profiles.active=default --spring.config.location=../yti-codelist-config/application.yml,../yti-codelist-config/yti-codelist-content-intake-service.yml`
- Workdir: `$MODULE_DIR$`

Add folder for yti-codelist -project, application writes modified files there:

```bash
$ mkdir /data/yti
```

### Building the Docker Image

```bash
$ mvn clean package docker:build
```

### Running the Docker Image

```bash
$ docker run --rm -p 9602:9602 -p 19602:19602 -v /path/to/yti-codelist-config:/config --name=yti-codelist-content-intake-service yti-codelist-content-intake-service -a --spring.config.location=/config/application.yml,/config/yti-codelist-content-intake-service.ym
```

.. or in [yti-codelist-compose](https://github.com/vrk-yti/yti-codelist-compose/) run

```bash
$ docker-compose up yti-codelist-content-intake-service
```

[Spring boot]:http://projects.spring.io/spring-boot/
[Jetty]:http://www.eclipse.org/jetty/
[Jersey 2]:https://jersey.java.net
