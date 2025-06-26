FROM ubuntu:latest

RUN apt-get update \
 && apt-get install -y --no-install-recommends git openjdk-21-jdk ca-certificates wget unzip \
 && rm -rf /var/lib/apt/lists/* \
 && rm -rf /usr/share/doc && rm -rf /usr/share/man \
 && apt-get clean

# Install a specific version of protoc for the templated plugins
WORKDIR /usr/dependencies
RUN mkdir /usr/dependencies/protoc \
    && wget https://github.com/protocolbuffers/protobuf/releases/download/v25.5/protoc-25.5-linux-x86_64.zip -O /usr/dependencies/protoc.zip \
    && unzip /usr/dependencies/protoc.zip -d /usr/dependencies/protoc/
ENV PATH="${PATH}:/usr/dependencies/protoc/bin"

# Install a specific version of Gradle
WORKDIR /usr/dependencies
RUN wget https://services.gradle.org/distributions/gradle-8.14.2-bin.zip -O /usr/dependencies/gradle.zip \
    && unzip /usr/dependencies/gradle.zip -d /usr/dependencies/ \
    && mv /usr/dependencies/gradle-8.14.2/ /usr/dependencies/gradle/
ENV PATH="${PATH}:/usr/dependencies/gradle/bin"
