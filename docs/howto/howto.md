# Build and run Tsunami

## Using Docker

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

Configuration files can be found in `/usr/tsunami/tsunami.yaml` for the scanner
and `/usr/tsunami/tcs_config.yaml` for the callback server.

Also note that to use the callback server, you might have to setup port
forwarding with your docker when starting it. We encourage you to refer to the
`-p` option of Docker.

## Development workflow

When using the default Docker image, you will notice that it gets rid of all
compilation artifacts before finalizing the image.

When you are in the middle of the development of a plugin, this might not be
very convenient; We generally recommend commenting out all of the `Stage 2` from
the Dockerfile. We also recommend commenting out the other plugins section so
that the compilation process is faster.

A few important things:

- Everything related to Tsunami is in `/usr/tsunami` (config, jar, ...);
- The source-code is contained in `/usr/tsunami/repos` where you can make your
changes;
- When you compile your plugin, you need to add it to `/usr/tsunami/plugins` so
that it is used by the `tsunami` wrapper;
