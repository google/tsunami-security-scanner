# How do I...

This page answers common how-to questions that may come up when using Tsunami.

## Content

How do I...

*   ... [build and execute the scanner?](#build_n_execute)
*   ... [install Tsunami plugins?](#install_plugins)
*   ... [create a new Tsunami plugin?](#create_plugins)
*   ...
    [apply my plugins to certain types of services / software?](#filter_plugins)
*   ... [add command line arguments for my plugin?](#command_line)
*   ... [add configuration properties for my plugin?](#configuration)

## <a name="build_n_execute"></a>... build and execute the scanner?

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

## <a name="install_plugins"></a>... install Tsunami plugins?

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

## <a name="create_plugins"></a>... create a new Tsunami plugin?

Follow examples from the
[tsunami-security-scanner-plugins](https://github.com/google/tsunami-security-scanner-plugins)
repo.

## <a name="filter_plugins"></a>... apply my plugins to certain types of services / software?

Tsunami supports several filtering annotations that can be applied to a plugin.
When used, plugins will only be selected for execution when the filtering
criteria is satisfied.

In the following example, a `ForServiceName` annotation is applied to the
`WebFingerprinter` plugin. `ForServiceName` annotation compares the
`service_name` field of the `NetworkService` protobuf with its target values.
The annotated plugin will only be selected for execution when there is a match.
Here the `WebFingerprinter` plugin will run when the scan target exposes either
a `http` or a `https` service.

```java
// ...
@ForServiceName(["http", "https"])
public final class WebFingerprinter implements ServiceFingerprinter {
  // ...
}
```

## <a name="command_line"></a>... add command line arguments for my plugin?

Tsunami uses [jCommander](https://jcommander.org/) for command line argument
parsing. In order to add new command line arguments for your plugin, first
define the data class for holding all the arguments. You can follow the
[jCommander](https://jcommander.org/) tutorial to learn more about the utility.

```java
@Parameters(separators = "=")
public class MyPluginArgs implements CliOption {
  @Parameter(names = "--param", description = "Description for param.")
  private String param;

  @Override
  public void validate() {
    // Validate the command line value.
  }
}
```

Then inject an instance of this data class into your plugin's constructor like
so:

```java
// ...
public final class MyPlugin implements VulnDetector {
  private final MyPluginArgs args;
  @Inject
  MyPlugin(MyPluginArgs args) {
    this.args = checkNotNull(args);
  }
  // ...
}
```

The scanner will automatically parse the command line arguments passed to the
binary, create an instance of the data class from parsed values, and inject the
instance into your plugin.

## <a name="configuration"></a>... add configuration properties for my plugin?

Similar to command line argument, you could add configuration properties for
your plugins and tweak configurations using a config file. Currently Tsunami
supports loading configs from a YAML file.

Tsunami uses
[snakeyaml](https://bitbucket.org/asomov/snakeyaml/wiki/Documentation) to parse
the YAML config file. In order to add configuration properties to your plugin,
first you need to define a data class for holding all the configuration values.
Currently Tsunami only supports standard Java data types for configuration like
strings, numbers (`int`, `long`, `float`, `double`, etc), lists and maps of
standard Java data types.

```java
// All config classes must be annotated by this ConfigProperties annotation in
// order for Tsunami to recognize the config class.
@ConfigProperties(prefix = "my.plugin.configs")
public class MyPluginConfigs {
  String stringValue;
  long longValue;
  List<String> listValues;
  Map<String, String> mapValues;
}
```

Then similar to the command line arguments, you can inject an instance of this
data class into your plugin's constructor like so:

```java
// ...
public final class MyPlugin implements VulnDetector {
  private final MyPluginConfigs configs;
  @Inject
  MyPlugin(MyPluginConfigs configs) {
    this.configs = checkNotNull(configs);
  }
  // ...
}
```

The scanner will parse the configuration file when it starts, create an instance
of the data class from the config data, and inject the instance into your
plugin.

Following is an example config file for the previously defined `MyPluginConfigs`
object.

```yaml
# tsunami.yaml file
my:
  plugin:
    configs:
      stringValue: "example value"
      long_value: 123
      list_values:
        - "a"
        - "b"
        - "c"
      mapValues:
        key1: "value1"
        key2: "value2"
```
