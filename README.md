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

To quickly get started with Tsunami scans, execute the following command.

```shell
bash -c "$(curl -sfL https://raw.githubusercontent.com/google/tsunami-security-scanner/master/quick_start.sh)"
```

The `quick_start.sh` script performs the following tasks:

1.  Clone the
    [google/tsunami-security-scanner](https://github.com/google/tsunami-security-scanner)
    and
    [google/tsunami-security-scanner-plugins](https://github.com/google/tsunami-security-scanner-plugins)
    repos into `$HOME/tsunami/repos` directory.
1.  Compile all
    [Google Tsunami plugins](https://github.com/google/tsunami-security-scanner-plugins/tree/master/google)
    and move all plugin `jar` files into `$HOME/tsunami/plugins` directory.
1.  Compile the Tsunami scanner Fat Jar file and move it into `$HOME/tsunami`
    directory.
1.  Move the `tsunami.yaml` example config into `$HOME/tsunami` directory.
1.  Print example Tsunami command for scanning `127.0.0.1` using the previously
    generated artifacts.

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
