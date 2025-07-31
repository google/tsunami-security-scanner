# Stage 1: Build phase

FROM ghcr.io/google/tsunami-scanner-devel:latest AS build

## build the core engine
WORKDIR /usr/repos/tsunami-security-scanner
COPY . .
RUN mkdir -p /usr/tsunami
RUN gradle shadowJar
RUN find . -name 'tsunami-main-*.jar' -exec cp {} /usr/tsunami/tsunami.jar \;
RUN cp ./tsunami_tcs.yaml /usr/tsunami/tsunami.yaml
RUN cp plugin/src/main/resources/com/google/tsunami/plugin/payload/payload_definitions.yaml /usr/tsunami/payload_definitions.yaml
RUN cp -r plugin_server/py/ /usr/tsunami/py_server

## We perform a hotpatch of the path pointing to the payload definitions file
## for easier usage in the Dockerized environment.
RUN sed -i "s%'../../plugin/src/main/resources/com/google/tsunami/plugin/payload/payload_definitions.yaml'%'/usr/tsunami/payload_definitions.yaml'%g" \
      /usr/tsunami/py_server/plugin/payload/payload_utility.py

## generate the protos for Python plugins
WORKDIR /usr/repos/tsunami-security-scanner/
RUN python3 -m grpc_tools.protoc \
  -I/usr/repos/tsunami-security-scanner/proto \
  --python_out=/usr/tsunami/py_server/ \
  --grpc_python_out=/usr/tsunami/py_server/ \
  /usr/repos/tsunami-security-scanner/proto/*.proto

# Stage 2: Release

FROM scratch AS release

COPY --from=build /usr/tsunami/tsunami.jar /usr/tsunami/
COPY --from=build /usr/tsunami/tsunami.yaml /usr/tsunami/
COPY --from=build /usr/tsunami/payload_definitions.yaml /usr/tsunami/payload_definitions.yaml

# Python server and the virtual environment
COPY --from=build /usr/tsunami/py_venv/ /usr/tsunami/py_venv
COPY --from=build /usr/tsunami/py_server/ /usr/tsunami/py_server
