#!/bin/bash
# Copyright 2024 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -eu

WD="${HOME}/tsunami"
REPOS="${WD}/repos"
PLUGINS="${WD}/plugins"
PROTOC="${WD}/protoc"

mkdir -p "${REPOS}"
mkdir -p "${PLUGINS}"
mkdir -p "${PROTOC}"

# Clone repos.
pushd "${REPOS}" >/dev/null

printf "\nFetching source code for Tsunami scanner ...\n"
if [[ ! -d "tsunami-security-scanner" ]] ; then
  git clone https://github.com/google/tsunami-security-scanner
else
  pushd "tsunami-security-scanner" >/dev/null
  git pull origin master
  popd >/dev/null
fi
printf "\nFetching source code for Tsunami scanner plugins ...\n"
if [[ ! -d "tsunami-security-scanner-plugins" ]] ; then
  git clone https://github.com/google/tsunami-security-scanner-plugins
else
  pushd "tsunami-security-scanner-plugins" >/dev/null
  git pull origin master
  popd >/dev/null
fi
printf "\nFetching source code for Tsunami scanner callback server ...\n"
if [[ ! -d "tsunami-security-scanner-callback-server" ]] ; then
  git clone https://github.com/google/tsunami-security-scanner-callback-server
else
  pushd "tsunami-security-scanner-callback-server" >/dev/null
  git pull origin master
  popd >/dev/null
fi
popd >/dev/null

# Build all google plugins.
pushd "${REPOS}/tsunami-security-scanner-plugins/google" >/dev/null
printf "\nBuilding all Google plugins ...\n"
./build_all.sh
cp build/plugins/*.jar "${PLUGINS}"
popd >/dev/null

# Copy over python plugins.
# Exclude the python example plugin.
pushd "${REPOS}/tsunami-security-scanner-plugins/py_plugins/" >/dev/null
mkdir -p "${REPOS}/tsunami-security-scanner/plugin_server/py/py_plugins"
for py_plugin in `find * -type f -name '*.py' -not -iname '*example_py_vuln_detector*' -not -iname '*_test.py'`; do
  cp $py_plugin "${REPOS}"/tsunami-security-scanner/plugin_server/py/py_plugins/`basename $py_plugin`
done
popd >/dev/null

# Build the callback server.
pushd "${REPOS}/tsunami-security-scanner-callback-server" >/dev/null
printf "\nBuilding Tsunami callback server ...\n"
./gradlew shadowJar
TCS_JAR=$(find "${REPOS}/tsunami-security-scanner-callback-server" -name 'tcs-main-*-cli.jar')
TCS_JAR_FILENAME=$(basename -- "${TCS_JAR}")
cp "${TCS_JAR}" "${WD}"
cp "${REPOS}/tsunami-security-scanner-callback-server/tcs_config.yaml" "${WD}"
popd >/dev/null

# Build the scanner.
pushd "${REPOS}/tsunami-security-scanner" >/dev/null
printf "\nBuilding Tsunami scanner jar file ...\n"
./gradlew shadowJar
JAR=$(find "${REPOS}/tsunami-security-scanner" -name 'tsunami-main-*-cli.jar')
JAR_FILENAME=$(basename -- "${JAR}")
cp "${JAR}" "${WD}"
cp "${REPOS}/tsunami-security-scanner/tsunami_tcs.yaml" "${WD}"
popd >/dev/null

# Install python libs and generate python proto targets.
pushd "${REPOS}/tsunami-security-scanner/plugin_server/py" >/dev/null
# sudo apt install python3.11-venv
python3 -m venv .
source bin/activate
pip install --require-hashes -r requirements.txt
popd >/dev/null

pushd "${REPOS}/tsunami-security-scanner/proto" >/dev/null
PROTO_OUT="${REPOS}/tsunami-security-scanner/plugin_server/py"
for proto in `ls *.proto`; do
  python -m grpc_tools.protoc -I. --python_out=${PROTO_OUT}/. --grpc_python_out=${PROTO_OUT}/. "${proto}"
done
popd >/dev/null

pushd "${REPOS}/tsunami-security-scanner-callback-server/proto" >/dev/null
python -m grpc_tools.protoc -I. --python_out=${PROTO_OUT}/. --grpc_python_out=${PROTO_OUT}/. "polling.proto"
popd >/dev/null

printf "\nBuild successful, execute the following command to start the callback server\n"
printf "\ncd ${WD} && \\\\\n"
printf "java -cp \"${TCS_JAR_FILENAME}\" \\\\\n"
printf "  com.google.tsunami.callbackserver.main.TcsMain \\\\\n"
printf "  --custom-config=tcs_config.yaml\n"

printf "\nBuild successful, execute the following command to start the pythan language server\n"
printf "\ncd ${REPOS}/tsunami-security-scanner/plugin_server/py && \\\\\n"
printf "python3 -m venv . && source bin/activate && \\\\\n"
printf "python3 plugin_server.py \\\\\n"
printf "  --port=34567 \\\\\n"
printf "  --trust_all_ssl_cert=true \\\\\n"
printf "  --timeout_seconds=180 \\\\\n"
printf "  --callback_address=127.0.0.1 \\\\\n"
printf "  --callback_port=8881 \\\\\n"
printf "  --polling_uri=http://127.0.0.1:8880\n"

printf "\nBuild successful, execute the following command to scan 127.0.0.1:\n"
printf "\ncd ${WD} && \\\\\n"
printf "java -cp \"${JAR_FILENAME}:${WD}/plugins/*\" \\\\\n"
printf "  -Dtsunami.config.location=${WD}/tsunami_tcs.yaml \\\\\n"
printf "  com.google.tsunami.main.cli.TsunamiCli \\\\\n"
printf "  --ip-v4-target=127.0.0.1 \\\\\n"
printf "  --scan-results-local-output-format=JSON \\\\\n"
printf "  --scan-results-local-output-filename=/tmp/tsunami-output.json \\\\\n"
printf "  --remote-plugin-server-addresses=127.0.0.1 \\\\\n"
printf "  --remote-plugin-server-ports=34567 \n"
