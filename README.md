# CLS (Code List Service) - Content Intake Service microservice  

This application is part of the [Joint metadata and information management programme](https://wiki.julkict.fi/julkict/yti).

## Description

This is the implementation of the Content Intake Service microservice for the Code List Service (CLS) with:

* [Spring boot] For getting things up and running
* Embedded [Jetty] to serve
* [Jersey 2] for JAX-RS

## Interface Documentation

When the microservice is running, you can get the Swagger REST API documentation from:
[http://localhost:9601/cls-intake/api/swagger.json](http://localhost:9601/cls-intake/api/swagger.json)
[http://localhost:9601/cls-intake/swagger/index.html](http://localhost:9601/cls-intake/swagger/index.html)

## Prerequisities

### Building
- Java 8+
- Maven 3.3+
- Docker

## Running

- [cls-config](https://github.com/vrk-yti/cls-config/) - Default configuration for development use.

## Starting service on local development environment

### Running inside IDE

Add the following Run configurations options:

- Program arguments: `--spring.profiles.active=default,local --spring.config.location=../cls-config/application.yml,../cls-config/cls-content-intake-service.yml`
- Workdir: `$MODULE_DIR$`


### Building the Docker Image

```bash
$ mvn clean package docker:build
```

### Running the Docker Image

```bash
$ docker run --rm -p 9601:9601 -p 19601:19601 -v /path/to/cls-config:/config --name=cls-content-intake-service cls-content-intake-service -a --spring.config.location=/config/application.yml,/config/cls-content-intake-service.ym
```

.. or in [cls-compose](https://github.com/vrk-yti/cls-compose/) run

```bash
$ docker-compose up cls-content-intake-service
```

[Spring boot]:http://projects.spring.io/spring-boot/
[Jetty]:http://www.eclipse.org/jetty/
[Jersey 2]:https://jersey.java.net
