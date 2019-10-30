# YTI CodeList - Content Intake Service microservice  

This application is part of the [Joint metadata and information management programme](https://wiki.julkict.fi/julkict/yti).

## Description

This is the implementation of the Content Intake Service microservice for the YTI CodeList (yti-codelist) with:

* [Spring boot] For getting things up and running
* Embedded [Tomcat] to serve
* [Jersey 2] for JAX-RS

## Interface Documentation

When the microservice is running, you can get the OpenAPI documentation from:
- [http://localhost:9602/codelist-intake/api/openapi.json](http://localhost:9602/codelist-intake/api/openapi.json)
- [http://localhost:9602/codelist-intake/api/openapi.yaml](http://localhost:9602/codelist-intake/api/openapi.yaml)
- [http://localhost:9602/codelist-intake/swagger/index.html](http://localhost:9602/codelist-intake/swagger/index.html)

## Prerequisities

### Building
- Java 8+
- Maven 3.3+
- Docker

## Running

- [yti-compose](https://github.com/vrk-yti/yti-compose/) - Default configuration for development use.

## Starting service on local development environment

### Running inside IDE

Add the following Run configurations options:

- Program arguments: `--spring.profiles.active=local --spring.config.location=../yti-compose/config/application.yml,../yti-compose/config/yti-codelist-content-intake-service.yml`
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
$ docker run --rm -p 9602:9602 -p 19602:19602 -v /path/to/yti-codelist-config:/config --name=yti-codelist-content-intake-service yti-codelist-content-intake-service -a --spring.config.location=/yti-compose/config/application.yml,/yti-compose/config/yti-codelist-content-intake-service.ym
```

.. or in [yti-compose](https://github.com/vrk-yti/yti-compose/) run

```bash
$ docker-compose up yti-codelist-content-intake-service
```

[Spring boot]:http://projects.spring.io/spring-boot/
[Tomcat]:https://tomcat.apache.org/
[Jersey 2]:https://jersey.java.net
