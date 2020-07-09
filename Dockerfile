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

RUN export TSUNAMI_JAR=$(find "./" -name 'tsunami-main-*-cli.jar')
RUN cp $TSUNAMI_JAR /usr/tsunami
RUN cp ./tsunami.yaml /usr/tsunami

CMD java -cp "${TSUNAMI_JAR}:/root/tsunami/plugins/*" -Dtsunami-config.location=/root/tsunami/tsunami.yaml com.google.tsunami.main.cli.TsunamiCli --ip-v4-target=$TARGET_IP