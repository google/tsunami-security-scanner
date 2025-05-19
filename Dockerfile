## Stage 1: Build phase

FROM ubuntu:latest

# Install dependencies
RUN apt-get update \
 && apt-get install -y --no-install-recommends git ca-certificates wget unzip openjdk-21-jdk \
 && rm -rf /var/lib/apt/lists/* \
 && rm -rf /usr/share/doc && rm -rf /usr/share/man \
 && apt-get clean

# Install a specific version of protoc
WORKDIR /usr/tsunami/deps
RUN mkdir /usr/tsunami/deps/protoc \
    && wget https://github.com/protocolbuffers/protobuf/releases/download/v25.5/protoc-25.5-linux-x86_64.zip -O /usr/tsunami/deps/protoc.zip \
    && unzip /usr/tsunami/deps/protoc.zip -d /usr/tsunami/deps/protoc/
ENV PATH="${PATH}:/usr/tsunami/deps/protoc/bin"

# Clone the plugins repo
WORKDIR /usr/tsunami/repos
RUN git clone --depth 1 "https://github.com/google/tsunami-security-scanner-plugins"

# Build templated plugins
WORKDIR /usr/tsunami/repos/tsunami-security-scanner-plugins/templated/templateddetector
RUN mkdir /usr/tsunami/plugins \
    && ./gradlew build \
    && cp build/libs/*.jar /usr/tsunami/plugins

# Build Google plugins
WORKDIR /usr/tsunami/repos/tsunami-security-scanner-plugins/google
RUN chmod +x build_all.sh \
    && ./build_all.sh \
    && cp build/plugins/*.jar /usr/tsunami/plugins

# Compile Tsunami
WORKDIR /usr/repos/tsunami-security-scanner
COPY . .
RUN ./gradlew shadowJar \
    && cp "$(find "./" -name "tsunami-main-*-all.jar")" /usr/tsunami/tsunami.jar \
    && cp ./tsunami.yaml /usr/tsunami

## Stage 2: Release

FROM ubuntu:latest

# Install dependencies
RUN apt-get update \
    && apt-get install -y --no-install-recommends nmap ncrack ca-certificates openjdk-21-jre \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /usr/share/doc && rm -rf /usr/share/man \
    && apt-get clean \
    && mkdir logs/

WORKDIR /usr/tsunami

COPY --from=0 /usr/tsunami /usr/tsunami

ENTRYPOINT ["java", "-cp", "tsunami.jar:plugins/*", "-Dtsunami.config.location=tsunami.yaml", "com.google.tsunami.main.cli.TsunamiCli"]
CMD ["--ip-v4-target=127.0.0.1", "--scan-results-local-output-format=JSON", "--scan-results-local-output-filename=logs/tsunami-output.json"]
