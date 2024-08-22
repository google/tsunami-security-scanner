# Common detector patterns

### Running only for a specific service

*Use case: I want my detector to only run for web applications or for
application X.*

There exist currently two way in Tsunami to filter the service type:

1. Using annotations (preferred)

The
[`@ForWebService`](https://github.com/google/tsunami-security-scanner/blob/master/plugin/src/main/java/com/google/tsunami/plugin/annotations/ForWebService.java)
and
[`@ForServiceName({"X", "Y"})`](https://github.com/google/tsunami-security-scanner/blob/master/plugin/src/main/java/com/google/tsunami/plugin/annotations/ForServiceName.java)
annotations can be used to instruct the core engine of Tsunami to only run this
plugin if the service was a web service or a service with name `X` or `Y`. The
name of the service is obtained during the discovery phase. It currently is the
exact same service name as NMAP would report (e.g. `http`, `https`, `ssh`).

2. Using filtering (web service only)

The
[`NetworkServiceUtils.isWebService()`](https://github.com/google/tsunami-security-scanner/blob/483f9ea5b7c69e8802353e0dcd293c2f35eaa4aa/common/src/main/java/com/google/tsunami/common/data/NetworkServiceUtils.java#L69)
can be used when performing filtering to ensure only `NetworkService` that were
identified as web service will be processed.

Example usage:

```java
someNetworkServiceCollection.stream()
  .filter(NetworkServiceUtils::isWebService)
  // {...}
```

### Building URLs

*Use case: My detector targets a web application. How do I build the URL?*

When writing your plugins, there are a few things that you should NOT have to
care about and that the Tsunami core engine should resolve for you:

- Is the service using HTTP or HTTPS?
- How do I construct the URL from the `NetworkService`?
- What if NMAP fails to identify the service as HTTP and I still want to build
  an URL?

All of these concerns are addressed in the core engine of Tsunami and you can
simply use the URL building API:
[`NetworkServiceUtils.buildWebApplicationRootUrl()`](https://github.com/google/tsunami-security-scanner/blob/483f9ea5b7c69e8802353e0dcd293c2f35eaa4aa/common/src/main/java/com/google/tsunami/common/data/NetworkServiceUtils.java#L173)

#### DO

```java
String myUrl = NetworkServiceUtils.buildWebApplicationRootUrl(networkService)
  + MY_VULNERABLE_ENDPOINT;
```

#### DO NOT

The following **SHOULD NOT BE USED**:

1. Defining a `buildTarget` intermediate function is redundant and most of the
time not necessary:

```java
 private static StringBuilder buildTarget(NetworkService networkService) {
    StringBuilder targetUrlBuilder = new StringBuilder();
    if (NetworkServiceUtils.isWebService(networkService)) {
      targetUrlBuilder.append(NetworkServiceUtils.buildWebApplicationRootUrl(networkService));
    } else {
      targetUrlBuilder
          .append("http://")
          .append(toUriAuthority(networkService.getNetworkEndpoint()))
          .append("/");
    }
    targetUrlBuilder.append(MY_VULNERABLE_ENDPOINT);
    return targetUrlBuilder;
  }
```

2. Using `String.Format` does not make use of the information obtained during
the discovery phase and is error prone:

```java
var uriAuthority = NetworkEndpointUtils.toUriAuthority(networkService.getNetworkEndpoint());
var loginPageUrl = String.format("http://%s/%s", uriAuthority, MY_VULNERABLE_ENDPOINT);
```

### Adding command line arguments consumed by the detector

*Use case: I need command line arguments for my detector*

Tsunami uses [jCommander](https://jcommander.org/) for command line argument
parsing. In order to add new CLI arguments for your plugin, first define the
data class for holding all the arguments. You can follow the jCommander tutorial
to learn more about this tool.

For example:

```java
@Parameters(separators = "=")
public final class MyPluginArgs implements CliOption {
  @Parameter(names = "--param", description = "Description for param.")
  public String param;

  @Override
  public void validate() {
    // Validate the command line value.
  }
}
```

Then, the CLI flags will be parsed and an instance of this class will be created
by the main scanner at start-up time. In order to use this class in your plugin,
you can directly inject this data class into your plugin's constructor.

```java
public final class MyVulnDetector implements VulnDetector {
  private final MyPluginArgs args;

  @Inject
  MyVulnDetector(MyPluginArgs args) {
    this.args = checkNotNull(args);
  }

  // {...}
}
```

### Adding configuration properties consumed by the detector

*Use case: How do I add configurable option for my detector?*

Tsunami supports loading configs from a YAML file and uses
[snakeyaml](https://bitbucket.org/asomov/snakeyaml/wiki/Documentation) to parse
the YAML config files. In order to add configuration properties to your plugin,
first you need to define a data class for holding all the configuration values.

NOTE: Currently Tsunami only supports standard Java data types for
configurations like `String`, numbers (`int`, `long`, `float`, `double`, etc),
`List` and `Map`.

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

Then, similar to the command line arguments, you can inject an instance of this
data class into your plugin's constructor.

```java
public final class MyVulnDetector implements VulnDetector {
  private final MyPluginConfigs configs;

  @Inject
  MyVulnDetector(MyPluginConfigs configs) {
    this.configs = checkNotNull(configs);
  }

  // {...}
}
```

The scanner will parse the configuration file when it starts, create an instance
of the data class from the config data, and inject the instance into your
plugin.

Following is an example config file for the previously defined `MyPluginConfigs`
object.

```yaml
my:
  plugin:
    configs:
      # Config name can be exact match.
      stringValue: "example value"
      # Or matching via snake_case.
      long_value: 123
      list_values:
      - "a"
      - "b"
      - "c"
      mapValues:
        key1: "value1"
        key2: "value2"
```

To use the configuration file, you need to set the `tsunami.config.location`
option when calling Tsunami.
