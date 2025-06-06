## Stage 1: Build phase

FROM ubuntu:latest

### 1.0. Dependencies

RUN apt-get update \
 && apt-get install -y --no-install-recommends git ca-certificates wget unzip openjdk-21-jdk \
 && rm -rf /var/lib/apt/lists/* \
 && rm -rf /usr/share/doc && rm -rf /usr/share/man \
 && apt-get clean

# Install a specific version of protoc for the templated plugins
WORKDIR /usr/tsunami/deps
RUN mkdir /usr/tsunami/deps/protoc \
    && wget https://github.com/protocolbuffers/protobuf/releases/download/v25.5/protoc-25.5-linux-x86_64.zip -O /usr/tsunami/deps/protoc.zip \
    && unzip /usr/tsunami/deps/protoc.zip -d /usr/tsunami/deps/protoc/
ENV PATH="${PATH}:/usr/tsunami/deps/protoc/bin"

### 1.1. Compile the core of Tsunami and the callback server

WORKDIR /usr/repos/tsunami-security-scanner
COPY . .
RUN mv /usr/repos/tsunami-security-scanner/tsunami-security-scanner-plugins /usr/repos/tsunami-security-scanner-plugins \
    && mv /usr/repos/tsunami-security-scanner/tsunami-security-scanner-callback-server /usr/repos/tsunami-security-scanner-callback-server
RUN ./gradlew shadowJar
RUN find . -name 'tsunami-main-*.jar' -exec cp {} /usr/tsunami/tsunami.jar \; \
    && cp ./tsunami_tcs.yaml /usr/tsunami/tsunami.yaml

WORKDIR /usr/repos/tsunami-security-scanner-callback-server
RUN ./gradlew shadowJar
RUN find . -name 'tcs-main-*.jar' -exec cp {} /usr/tsunami/tsunami-tcs.jar \; \
    && cp tcs_config.yaml /usr/tsunami/tcs_config.yaml

### 1.2. Compile the plugins

# Build templated plugins
WORKDIR /usr/repos/tsunami-security-scanner-plugins/templated/templateddetector
RUN mkdir /usr/tsunami/plugins && ./gradlew build
RUN cp build/libs/*.jar /usr/tsunami/plugins

# Build Google plugins
WORKDIR /usr/repos/tsunami-security-scanner-plugins/google
RUN chmod +x build_all.sh \
    && ./build_all.sh
RUN cp build/plugins/*.jar /usr/tsunami/plugins

# Build all other plugins
WORKDIR /usr/repos/tsunami-security-scanner-plugins/community
RUN chmod +x build_all.sh \
    && ./build_all.sh
RUN cp build/plugins/*.jar /usr/tsunami/plugins

WORKDIR /usr/repos/tsunami-security-scanner-plugins/doyensec
RUN chmod +x build_all.sh \
    && ./build_all.sh
RUN cp build/plugins/*.jar /usr/tsunami/plugins

WORKDIR /usr/repos/tsunami-security-scanner-plugins/govtech
RUN chmod +x build_all.sh \
    && ./build_all.sh
RUN cp build/plugins/*.jar /usr/tsunami/plugins

## Stage 2: Release

FROM ubuntu:latest

# Install dependencies
RUN apt-get update \
    && apt-get install -y --no-install-recommends nmap ncrack ca-certificates openjdk-21-jre golang \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /usr/share/doc && rm -rf /usr/share/man \
    && apt-get clean \
    && mkdir logs/

# Create wrapper scripts
WORKDIR /usr/tsunami
RUN echo '#!/bin/bash\njava -cp /usr/tsunami/tsunami.jar:/usr/tsunami/plugins/* -Dtsunami.config.location=/usr/tsunami/tsunami.yaml com.google.tsunami.main.cli.TsunamiCli $*\n' > /usr/bin/tsunami \
    && chmod +x /usr/bin/tsunami \
    && echo '#!/bin/bash\njava -cp /usr/tsunami/tsunami-tcs.jar com.google.tsunami.callbackserver.main.TcsMain --custom-config=/usr/tsunami/tcs_config.yaml $*\n' > /usr/bin/tsunami-tcs \
    && chmod +x /usr/bin/tsunami-tcs
# Install the linter
RUN go install github.com/google/tsunami-security-scanner-plugins/templated/utils/linter@latest \
    && ln -s /root/go/bin/linter /usr/bin/tsunami-linter

# Copy previous build results
COPY --from=0 /usr/tsunami /usr/tsunami
