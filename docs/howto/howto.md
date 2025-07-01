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
COPY --from=full /usr/tsunami /usr/tsunami/
COPY --from=full /usr/bin/tsunami /usr/bin/tsunami
COPY --from=full /usr/bin/tsunami-tcs /usr/bin/tsunami-tcs
RUN rm -f /usr/tsunami/plugins/*
```

You can then build that image and use it with your local copy of the plugins,
where `/path/to/my/plugin` must point to your plugin. This is usually the folder
containing your `build.gradle` if using Java or the `templated` folder of the
plugins repository if using templated plugins.

```sh
$ docker build -t tsunadev:latest . -f myDockerfile
$ docker run -it --rm -v /path/to/my/plugin:/usr/tsunami/repos/myplugin tsunadev:latest bash

## Java plugins
(docker) $ cd /usr/tsunami/repos/myplugin
(docker) $ gradle build
(docker) $ cp build/libs/*.jar /usr/tsunami/plugins

## Templated plugins
(docker) $ cd /usr/tsunami/repos/myplugin/templateddetector
(docker) $ gradle build
(docker) $ cp build/libs/*.jar /usr/tsunami/plugins

## Once the plugin is added, you can run Tsunami
(docker) $ tsunami --ip-v4-target=127.0.0.1
```
