# Tsunami

![build](https://github.com/google/tsunami-security-scanner/actions/workflows/core-build.yml/badge.svg)

Tsunami is a general purpose network security scanner with an extensible plugin
system for detecting high severity vulnerabilities with high confidence.

To learn more about Tsunami, visit our
[documentation](https://google.github.io/tsunami-security-scanner/).

Tsunami relies heavily on its plugin system to provide basic scanning
capabilities. All publicly available Tsunami plugins are hosted in a separate
[google/tsunami-security-scanner-plugins](https://github.com/google/tsunami-security-scanner-plugins)
repository.

## Quick start

We provide a set of Docker images to help you build and use Tsunami. We provide
a minimal (scratch) image for:

- The core engine only;
- The callback server only;
- Each category of plugin;

Using these minimal images is not recommended, instead we recommend composing
on top of them.

If you just intend to run Tsunami, we recommend using the latest complete
image:

```sh
$ docker pull ghcr.io/google/tsunami-scanner-full:latest
$ docker run -it --rm ghcr.io/google/tsunami-scanner-full bash

# note: you will need to install a port scanner and a credential brute-forcer.
# We recommend installing nmap and ncrack using apt.

$ tsunami --ip-v4-target=127.0.0.1 ## starts tsunami
(docker) $ tsunami-tcs ## runs the callback server
(docker) $ tsunami-linter ## linter for the templated language
```

For more information, please see our documentation about
[building and running Tsunami](https://google.github.io/tsunami-security-scanner/howto/howto)

## Contributing

Read how to
[contribute to Tsunami](https://google.github.io/tsunami-security-scanner/contribute/).

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
