FROM openjdk:19-jdk-alpine3.16

RUN apk update && apk add curl

WORKDIR /app

COPY build/distributions/simple-service-1.0-SNAPSHOT.tar .

RUN tar -xf /app/simple-service-1.0-SNAPSHOT.tar -C /app

EXPOSE 8000

HEALTHCHECK CMD curl --fail http://localhost:8000 || exit 1

ENTRYPOINT ["sh", "/app/simple-service-1.0-SNAPSHOT/bin/simple-service"]



