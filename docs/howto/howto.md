# Build and run Tsunami

## Tsunami docker's environment

We provide a set of Docker images to help you build and use Tsunami. We provide
a minimal (scratch) image for:

- The core engine only;
- The callback server only;
- Each category of plugin;

Using these minimal images is not recommended, instead we recommend composing
on top of them.

![docker-images](img/docker-images.png)

## Running the latest version of Tsunami

If you just want to run the latest version of Tsunami, without having to
recompile anything, you can directly use the latest full image of Tsunami.

```sh
# Important: If you built a local version of the container, do not pull as it
# will overwrite your changes. Otherwise, do pull as you would be using a stale
# version of the image.
$ docker pull ghcr.io/google/tsunami-scanner-full

# Run the image
$ docker run -it --rm ghcr.io/google/tsunami-scanner-full bash

# If you want to use Python plugins
(docker) $ tsunami-py-server >/tmp/py_server.log 2>&1 &

# If you want to use the callback server
(docker) $ tsunami-tcs >/tmp/tcs_server.log 2>&1 &

# Run Tsunami
# Note: If you did not start the python server, omit the `--python-` arguments.
(docker) $ tsunami --ip-v4-target=127.0.0.1 --python-plugin-server-address=127.0.0.1 --python-plugin-server-port=34567
```

This images contains everything necessary under the `/usr/tsunami` directory.

To use the callback server, you might have to setup port forwarding with your
docker container when starting it. We encourage you to refer to the `-p` option
of Docker.

A few tips:

- Only scan one port: `--port-ranges-target`
- Only run your detector: `--detectors-include="detector-name"`; where detector
name is the name defined in `PluginInfo` section for Java and Python plugins and
the `info.name` section on templated plugins.

## Using docker to build Tsunami

In this section, we go through the different ways to compile the core engine
or a plugin locally so that you can test your changes.

It assumes that you have cloned both the `tsunami-security-scanner` and
`tsunami-security-scanner-plugins` repositories.

### Rebuilding the core engine

If you need to make changes to the core engine during the development cycle, you
will have to perform the following actions to test your change:

- Rebuild the core engine container;
- If your change required changing plugins: you will have to rebuild their
associated container as well;
- Rebuild the `-full` container;
- Run the scanner to check that everything works.

```sh
# Build the core engine container
$ cd tsunami-security-scanner
$ docker build -t ghcr.io/google/tsunami-scanner-core:latest -f core.Dockerfile .

# (Optional) Rebuild affected plugins
# See "Rebuilding a whole category of plugins" section on this page and do it
# for every category that is affected by your change.

# Build the full container
$ docker build -t ghcr.io/google/tsunami-scanner-full:latest -f full.Dockerfile .

# See the "Running the latest version of Tsunami" section on this page to run
# Tsunami with the newly built image. DO NOT perform a docker pull.
```

### Rebuilding a whole category of plugins

Tsunami groups plugins per categories. From the root folder of the plugin
repository, you can see that the categories are `google`, `community`,
`templated` and so on.

Our docker images are built separately for each category. The same Dockerfile
is used, but it is parameterized to use a different folder with
`TSUNAMI_PLUGIN_FOLDER`.

```sh
$ cd tsunami-security-scanner-plugins
$ build -t ghcr.io/google/tsunami-plugins-category:latest --build-arg TSUNAMI_PLUGIN_FOLDER=category .

# For example with the community category:
$ build -t ghcr.io/google/tsunami-plugins-community:latest --build-arg TSUNAMI_PLUGIN_FOLDER=community .
```

For **Python plugins**, you need to use the dedicated Dockerfile, which only
supports bundling all plugins:

```sh
$ cd tsunami-security-scanner-plugins
$ build -t ghcr.io/google/tsunami-plugins-python:latest -f python.Dockerfile .
```

Once you have rebuilt the categories that you need, you can rebuild the `-full`
image:

```sh
$ cd tsunami-security-scanner
$ docker build -t ghcr.io/google/tsunami-scanner-full:latest -f full.Dockerfile .
```

Then follow "Running the latest version of Tsunami" to use this new image. DO
NOT perform a `docker pull`.

### Building an image for one plugin

Now, if during development you only wish to build your plugin, you can do so
by creating a new local-only category.

Before you start, you will need to change the definition of the
`full.Dockerfile` file:

- Add a `FROM` directive in the Plugins section:

```diff
FROM ghcr.io/google/tsunami-plugins-python:latest AS plugins-python
+ FROM ghcr.io/google/tsunami-plugins-local:latest AS plugins-local
```

- Add a `COPY` directive in the section that copies everything:

```diff
COPY --from=plugins-python /usr/tsunami/py_plugins/ /usr/tsunami/py_plugins/
+ COPY --from=plugins-local /usr/tsunami/plugins/ /usr/tsunami/plugins/
```

Then, you can build the actual image containing only your plugin:

```sh
$ cd tsunami-security-scanner-plugins
$ build -t ghcr.io/google/tsunami-plugins-local:latest --build-arg TSUNAMI_PLUGIN_FOLDER=path/to/my/plugin .
```

Finally, compile the `-full` image:

```sh
$ cd tsunami-security-scanner
$ docker build -t ghcr.io/google/tsunami-scanner-full:latest -f full.Dockerfile .
```

Then follow "Running the latest version of Tsunami" to use this new image. DO
NOT perform a `docker pull`.

**Python plugins** do not support building only one plugin. See building the
whole category instead.

## Building Tsunami without docker

We do not provide support for building Tsunami outside of our docker
environment.

You can use the Dockerfile provided in the repositories to build your own
toolchain if you so wish.
