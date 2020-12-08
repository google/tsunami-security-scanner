FROM openjdk:13-jdk-slim-buster

# Install dependencies
RUN apt-get update \
 && apt-get install -y --no-install-recommends nmap ncrack git ca-certificates \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /usr/tsunami/repos

# Clone the plugins repo
RUN git clone --depth 1 "https://github.com/google/tsunami-security-scanner-plugins"

# Build plugins
WORKDIR /usr/tsunami/repos/tsunami-security-scanner-plugins/google
RUN chmod +x build_all.sh \
    && ./build_all.sh

RUN mkdir /usr/tsunami/plugins
RUN cp build/plugins/*.jar /usr/tsunami/plugins

# Compile the Tsunami scanner
WORKDIR /usr/repos/tsunami-security-scanner
COPY . .
RUN ./gradlew shadowJar

RUN cp $(find "./" -name 'tsunami-main-*-cli.jar') /usr/tsunami/tsunami.jar
RUN cp ./tsunami.yaml /usr/tsunami

WORKDIR /usr/tsunami

RUN mkdir logs/

ENTRYPOINT ["java", "-cp", "tsunami.jar:plugins/*", "-Dtsunami-config.location=tsunami.yaml", "com.google.tsunami.main.cli.TsunamiCli"]
CMD ["--ip-v4-target=127.0.0.1", "--scan-results-local-output-format=JSON", "--scan-results-local-output-filename=logs/tsunami-output.json"]
