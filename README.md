# Tsunami

Tsunami is a general purpose network security scanner with an extensible plugin
system for detecting high severity vulnerabilities with high confidence.

To learn more about Tsunami, visit our
[documentations](https://github.com/google/tsunami-security-scanner/blob/master/docs/index.md).

Tsunami relies heavily on its plugin system to provide basic scanning
capabilities. All publicly available Tsunami plugins are hosted in a separate
[google/tsunami-security-scanner-plugins](https://github.com/google/tsunami-security-scanner-plugins)
repository.

## Current Status

*   Currently Tsunami is in 'pre-alpha' release for developer preview.
*   Tsunami project is currently under active development. Do expect major API
    changes in the future.

## Quick Start

To quickly get started with Tsunami scans,

1.  start a vulnerable application that can be identified by Tsunami, e.g. an
    unauthenticated Jupyter Notebook server. The easiest way is to use a docker
    image:

    ```shell
    docker run --name unauthenticated-jupyter-notebook -p 8888:8888 -d jupyter/base-notebook start-notebook.sh --NotebookApp.token=''
    ```

1.  build the docker image for Tsunami:

    ```
    docker build -t tsunami .
    ```

1. run the Tsunami image. The logs can be saved to the host machine by mounting a volume:

    ```
    docker run  --network="host" -v "$(pwd)/logs":/usr/tsunami/logs tsunami
    ```

## Contributing

Read how to [contribute to Tsunami](docs/contributing.md).

## License

Tsunami is released under the [Apache 2.0 license](LICENSE).

```
Copyright 2019 Google Inc.

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
