# Stage 1: Build phase

FROM ubuntu:latest AS build

## Dependencies

RUN apt-get update \
 && apt-get install -y --no-install-recommends openjdk-21-jdk \
 && rm -rf /var/lib/apt/lists/* \
 && rm -rf /usr/share/doc && rm -rf /usr/share/man \
 && apt-get clean

## Build the core engine

WORKDIR /usr/repos/tsunami-security-scanner
COPY . .
RUN mkdir -p /usr/tsunami
RUN ./gradlew shadowJar
RUN find . -name 'tsunami-main-*.jar' -exec cp {} /usr/tsunami/tsunami.jar \;
RUN cp ./tsunami_tcs.yaml /usr/tsunami/tsunami.yaml

# Stage 2: Release

FROM scratch AS release

# Copy previous build results
COPY --from=build /usr/tsunami/tsunami.jar /usr/tsunami/
COPY --from=build /usr/tsunami/tsunami.yaml /usr/tsunami/
