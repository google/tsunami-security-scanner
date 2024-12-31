# Build and run Tsunami

## Build and run the scanner

To build the scanner, go to the root path of the project and execute the
following command:

```shell
./gradlew shadowJar
```

When the command finishes, the generated scanner `jar` file is located in the
`main/build/libs` folder with the name of `tsunami-main-[version]-cli.jar`. This
is a fat jar file so can be treated as a standalone binary.

To execute the scanner, first you need to install plugins into a chosen folder.
The minimal required plugin is a `PortScanner` plugin.

Assuming plugins are installed under `~/tsunami-plugins/`, then you could use
the following command to execute a Tsunami scan:

```shell
java \
    # Tsunami classpath, as of now plugins must be installed into classpath.
    -cp "tsunami-main-[version]-cli.jar:~/tsunami-plugins/*" \
    # Specify the config file of Tsunami, by default Tsunami loads a tsunami.yaml
    # file from there the command is executed.
    -Dtsunami.config.location=/path/to/config/tsunami.yaml \
    # Main class for TsunamiCli.
    com.google.tsunami.main.cli.TsunamiCli \
    # Scan target.
    --ip-v4-target=127.0.0.1 \
    # Scan output file and data format.
    --scan-results-local-output-format=JSON \
    --scan-results-local-output-filename=/tmp/tsunami-result.json
```

NOTE: Currently Tsunami only supports loading plugins from its `classpath`. We
are adding new features to allow users specifying plugin installation folders in
the config file of Tsunami.

## Install Tsunami plugins

As mentioned above, Tsunami plugins must be installed into a folder that can be
recognized by Tsunami at runtime. This directory can be any arbitrary folder as
long as it is present in the runtime classpath of Tsunami.

Usually each Tsunami plugin is a standalone `jar` file. You can put whatever
support plugin `jar` file into the chosen directory. For example, if
`~/tsunami-plugins/` is where all plugins are installed, then the expected
layout of `~/tsunami-plugins/` would be:

```
$ ls ~/tsunami-plugins

awesome-port-scanner.jar         my-web-fingerprinter.jar      weak-ssh-cred-detector.jar
wordpress-installation.jar       exposed-jupyter-notebook.jar
```

NOTE: We are adding new features to allow users specifying plugin installation
folders in the config file of Tsunami.
