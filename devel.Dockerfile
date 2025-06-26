FROM ubuntu:latest

RUN apt-get update \
 && apt-get install -y --no-install-recommends git openjdk-21-jdk ca-certificates wget unzip \
 && rm -rf /var/lib/apt/lists/* \
 && rm -rf /usr/share/doc && rm -rf /usr/share/man \
 && apt-get clean

# Install a specific version of protoc for the templated plugins
WORKDIR /usr/tsunami/deps
RUN mkdir /usr/tsunami/deps/protoc \
    && wget https://github.com/protocolbuffers/protobuf/releases/download/v25.5/protoc-25.5-linux-x86_64.zip -O /usr/tsunami/deps/protoc.zip \
    && unzip /usr/tsunami/deps/protoc.zip -d /usr/tsunami/deps/protoc/
ENV PATH="${PATH}:/usr/tsunami/deps/protoc/bin"

# Install a specific version of Gradle
WORKDIR /usr/tsunami/deps
RUN wget https://services.gradle.org/distributions/gradle-8.14.2-bin.zip -O /usr/tsunami/deps/gradle.zip \
    && unzip /usr/tsunami/deps/gradle.zip -d /usr/tsunami/deps/ \
    && mv /usr/tsunami/deps/gradle-8.14.2/ /usr/tsunami/deps/gradle/
ENV PATH="${PATH}:/usr/tsunami/deps/gradle/bin"
