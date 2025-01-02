# music-release-radar-service

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/music-release-radar-service-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Running the application with Docker

### Step 1: Create a Docker network and configure the PostgreSQL container

A custom Docker network is required to enable communication between the PostgreSQL container and the application container. The PostgreSQL container needs to be connected to the same network as the application container, so they can communicate using container names instead of IP addresses.

This configuration allows the application container to resolve `postgres-db` to the PostgreSQL container within the same Docker network:
```properties
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://postgres-db:5432/music_release_radar_service_db
```

Create the Docker network:

```bash
docker network create music-net
```

Then, start the PostgreSQL container with the following command, ensuring it is connected to the `music-net` network:

```bash
docker run -d
    --name postgres-db
    --network music-net
    -e POSTGRES_USER=quarkus
    -e POSTGRES_PASSWORD=quarkus
    -e POSTGRES_DB=music_release_radar_service_db
    postgres:16
```

### Step 2: Build and run the application container

Build the application Docker image:

```bash
docker build -f src/main/docker/Dockerfile.jvm -t music-release-radar-service .
```

Start the application container, linking it to the `music-net` network:

```bash
docker run -d
    --name music-release-radar-service
    --network music-net
    -e QUARKUS_PROFILE=prod
    -p 8080:8080
    music-release-radar-service
```

### Step 3: Manage and verify containers

- verify running containers:
```bash
docker ps
```

- view logs of containers to ensure they started correctly:

```bash
docker logs <container_id>
```

- stop and remove the containers:

```bash
docker stop music-release-radar-service postgres-db
docker rm music-release-radar-service postgres-db
```

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
