# Stage 1: Build phase

FROM ghcr.io/google/tsunami-scanner-devel:latest AS build

## build the core engine
WORKDIR /usr/repos/tsunami-security-scanner
COPY . .
RUN mkdir -p /usr/tsunami
RUN gradle shadowJar
RUN find . -name 'tsunami-main-*.jar' -exec cp {} /usr/tsunami/tsunami.jar \;
RUN cp ./tsunami_tcs.yaml /usr/tsunami/tsunami.yaml

# Stage 2: Release

FROM scratch AS release

COPY --from=build /usr/tsunami/tsunami.jar /usr/tsunami/
COPY --from=build /usr/tsunami/tsunami.yaml /usr/tsunami/
