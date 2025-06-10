# Tsunami

![build](https://github.com/google/tsunami-security-scanner/workflows/build/badge.svg)

Tsunami is a general purpose network security scanner with an extensible plugin
system for detecting high severity vulnerabilities with high confidence.

To learn more about Tsunami, visit our
[documentation](https://google.github.io/tsunami-security-scanner/).

Tsunami relies heavily on its plugin system to provide basic scanning
capabilities. All publicly available Tsunami plugins are hosted in a separate
[google/tsunami-security-scanner-plugins](https://github.com/google/tsunami-security-scanner-plugins)
repository.

## Current Status

*   Currently Tsunami is in 'pre-alpha' release for developer preview.
*   Tsunami project is currently under active development. Do expect major API
    changes in the future.

## Quick Start

For simplicity, we provide a Dockerfile that should cover most of the use
cases.

You need to check-out the plugins and the callback server of Tsunami in the
root directory, next to the Dockerfile. We do not perform this step in the
Dockerfile so that you can modify plugins or the callback server configuration
easily during the development phase.

```
$ git clone https://github.com/google/tsunami-security-scanner-plugins
$ git clone https://github.com/google/tsunami-security-scanner-callback-server
$ docker build -t tsunami:latest .
```

You will then be able to use the docker image, for example:

```
$ docker run -it --rm tsunami:latest bash
(docker) # tsunami --ip-v4-target=127.0.0.1 ## starts tsunami
(docker) # tsunami-tcs ## runs the callback server
(docker) # tsunami-linter ## linter for the templated language
```

Whenever you make a change to a plugin, you need to reiterate the build phase.
While in the middle of development, we recommend commenting out the Stage 2 of
the Dockerfile so that you can directly build changed plugins directly in the
Docker.

Configuration files can be found in `/usr/tsunami/tsunami.yaml` for the scanner
and `/usr/tsunami/tcs_config.yaml` for the callback server.

Also note that to use the callback server, you might have to setup port forward
with your docker when starting it. We encourage you to refer to the `-p` option
of Docker.

## Contributing

Read how to [contribute to Tsunami](docs/contribute/contributing.md).

## License

Tsunami is released under the [Apache 2.0 license](LICENSE).

```
Copyright 2025 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Disclaimers

Tsunami is not an official Google product.
