#!/bin/bash
# Copyright 2020 Google LLC
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

mkdir -p "${REPOS}"
mkdir -p "${PLUGINS}"

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
popd >/dev/null

# Build all plugins.
pushd "${REPOS}/tsunami-security-scanner-plugins/google" >/dev/null
printf "\nBuilding all Google plugins ...\n"
./build_all.sh
cp build/plugins/*.jar "${PLUGINS}"
popd >/dev/null

# Build the scanner.
pushd "${REPOS}/tsunami-security-scanner" >/dev/null
printf "\nBuilding Tsunami scanner jar file ...\n"
./gradlew shadowJar
JAR=$(find "${REPOS}/tsunami-security-scanner" -name 'tsunami-main-*-cli.jar')
JAR_FILENAME=$(basename -- "${JAR}")
cp "${JAR}" "${WD}"
cp "${REPOS}/tsunami-security-scanner/tsunami.yaml" "${WD}"
popd >/dev/null

printf "\nBuild successful, execute the following command to scan 127.0.0.1:\n"
printf "\ncd ${WD} && \\\\\n"
printf "java -cp \"${JAR_FILENAME}:${WD}/plugins/*\" \\\\\n"
printf "  -Dtsunami-config.location=${WD}/tsunami.yaml \\\\\n"
printf "  com.google.tsunami.main.cli.TsunamiCli \\\\\n"
printf "  --ip-v4-target=127.0.0.1 \\\\\n"
printf "  --scan-results-local-output-format=JSON \\\\\n"
printf "  --scan-results-local-output-filename=/tmp/tsunami-output.json\n"
