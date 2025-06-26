# Build and run Tsunami

## Using Docker

We provide a set of Docker images to help you build and use Tsunami. We provide
a minimal (scratch) image for:

- The core engine only;
- The callback server only;
- Each category of plugin;

Using these minimal images is not recommended, instead we recommend composing
on top of them.

![docker-images](img/docker-images.png)

If you just intend to run Tsunami, we recommend using the latest complete
image:

```sh
$ docker run -it --rm ghcr.io/google/tsunami-scanner-full bash

# note: you will need to install a port scanner and a credential brute-forcer.
# We recommend installing nmap and ncrack using apt.

$ tsunami --ip-v4-target=127.0.0.1 ## starts tsunami
(docker) $ tsunami-tcs ## runs the callback server
(docker) $ tsunami-linter ## linter for the templated language
```

Configuration files can be found in `/usr/tsunami/tsunami.yaml` for the scanner
and `/usr/tsunami/tcs_config.yaml` for the callback server.

Also note that to use the callback server, you might have to setup port
forwarding with your docker when starting it. We encourage you to refer to the
`-p` option of Docker.

## Development workflow

To set-up your own development workflow, we recommend composing on top of the
tsunami full image but to delete existing plugins to minimize noise:

```dockerfile
FROM ghcr.io/google/tsunami-scanner-full:latest AS full
FROM ghcr.io/google/tsunami-scanner-devel:latest AS devel

WORKDIR /usr/tsunami
COPY --from=core /usr/tsunami/* /usr/tsunami
RUN rm -f /usr/tsunami/plugins/*
```

You can then build that image and use it with your local copy of the plugins:

```sh
$ docker build -t tsunadev:latest . -f myDockerfile
$ docker run -it --rm tsunadev:latest -v /path/to/tsunami-security-scanner-plugins:/usr/tsunami/repos/plugins bash
(docker) $ cd /usr/tsunami/repos/plugins/to/my/plugin
(docker) $ ./gradlew build
(docker) $ cp build/libs/*.jar /usr/tsunami/plugins
(docker) $ tsunami --ip-v4-target=127.0.0.1
```
