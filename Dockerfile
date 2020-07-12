# docker build -t tsunami -f Dockerfile .
# docker run --rm -it --network=host -v /tmp:/tmp:rw tsunami

FROM ubuntu:20.04

COPY . /root/tsunami

WORKDIR /root/tsunami

RUN apt-get update

RUN DEBIAN_FRONTEND=noninteractive apt-get install -y \
    git \
    curl \
    ncrack \
    openjdk-14-jdk \
    nmap

RUN ./quick_start.sh

CMD echo "Example command: java -cp "tsunami-main-0.0.2-SNAPSHOT-cli.jar:/root/tsunami/plugins/*"   -Dtsunami-config.location=/root/tsunami/tsunami.yaml   com.google.tsunami.main.cli.TsunamiCli   --ip-v4-target=127.0.0.1   --scan-results-local-output-format=JSON   --scan-results-local-output-filename=/tmp/tsunami-output.json" && bash -il

