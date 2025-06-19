# Writing a Tsunami detector (Java)

NOTE: We now expect you to write plugins using the templated format first. Only
resort to Java or Python if the plugin cannot be written with the templated
format.

## Overview

Each Tsunami detector needs the following pieces which we will create in this
tutorial:

*   A plugin name that is unique among all enabled Tsunami plugins.
*   A set of build rules for [Gradle](https://gradle.org/) (external build)
*   A `VulnDetector` that implements the vulnerability detection logic.
*   A `PluginBootstrapModule` that provides necessary Guice bindings for the
    detector.
*   An optional `CliOption` that captures all the supported command line flags
    for the detector.
*   An optional `ConfigProperties` that captures all the supported configuration
    for the detector.

## 1. Fork the examples

Tsunami provides a few example implementations of a `VulnDetector` plugin. The
examples live in the
[examples directory](https://github.com/google/tsunami-security-scanner-plugins/tree/master/examples)

*   Update Java package names. The example `VulnDetector` plugin is defined
    under `com.google.tsunami.plugins.example` package. Refactor the package and
    class name according to your detector implementation.
*   Give a meaningful description to the Gradle build rule at `build.gradle`. We
    will work on the Gradle build rule later.
*   Rewrite the `README.md` file to have a good explanation of your
    `VulnDetector` plugin.

## 2. Putting the detector together

### 2.a - `PluginInfo` annotation

All Tsunami plugins must be annotated by the `PluginInfo` annotation. Otherwise
it cannot be identified by Tsunami scanner at runtime. This annotation provides
the general information about the plugin to the core scanner.

Following is an example usage of the `PluginInfo` annotation from our
`WordPressInstallPageDetector`.

```java
@PluginInfo(
    // VULN_DETECTION PluginType is required for a VulnDetector plugin.
    type = PluginType.VULN_DETECTION,
    // This gives a human readable name for your VulnDetector. Can be different
    // from your class name.
    name = "WordPressInstallPageDetector",
    // The current version of your plugin.
    version = "0.1",
    // A detailed description about what this plugin does.
    description =
        "This detector checks whether a WordPress install is unfinished. An unfinished WordPress"
            + " installation exposes the /wp-admin/install.php page, which allows attacker to set"
            + " the admin password and possibly compromise the system.",
    // The author of this plugin.
    author = "Tsunami Team (tsunami-dev@google.com)",
    // The guice module that bootstraps this plugin.
    bootstrapModule = WordPressInstallPageDetectorBootstrapModule.class)
```

### 2.b - Define the `VulnDetector` plugin

Each vulnerability detector plugin is an implementation of the `VulnDetector`
interface. For this step we only explain the placeholder code, later you'll need
to provide implementations for the class itself.

Following is an example placeholder code from the
`WordPressInstallPageDetector`:

```java
// ...
// annotations
// ...
public final class WordPressInstallPageDetector implements VulnDetector {
  // See https://github.com/google/flogger for details.
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  // Tsunami uses Guice (https://github.com/google/guice) to manage the
  // dependencies.
  @Inject
  WordPressInstallPageDetector(
    // Tsunami provides a UtcClock for production and FakeUtcClock for
    // testing purposes.
    @UtcClock Clock utcClock,
    // You can also inject other useful dependencies to your plugin code, e.g.
    // inject HttpClient if you need to interact with a web server.
    HttpClient httpClient) {
  }

  // The entrypoint of the VulnDetector. We will explain this later.
  @Override
  public DetectionReportList detect(
    TargetInfo targetInfo, ImmutableList<NetworkService> matchedServices) {
    // implement me.
  }
}
```

### 2.c - Implement the main detection logic

The main logic of the detection is expected to happen in the `detect` method,
which expects two arguments:

1.  [The `TargetInfo` proto](https://github.com/google/tsunami-security-scanner/blob/master/proto/reconnaissance.proto).
   This proto contains information that were gathered during the fingerprinting
   and discovery phase.
1.  [The `NetworkService` list](https://github.com/google/tsunami-security-scanner/blob/master/proto/network_service.proto).
    This list contains all the identified network services that match the
    service filtering annotations.

The main detection logic usually iterates over all the elements of the
`matchedServices` parameter and checks whether the `NetworkService` is
vulnerable to the vulnerability your plugin checks for. If any service is
vulnerable, you'll need to build a `DetectionReport` proto that explains the
identified vulnerability.

Following is an example implementation from our `WordPressInstallPageDetector`:

```java
@Override
public DetectionReportList detect(
    TargetInfo targetInfo, ImmutableList<NetworkService> matchedServices) {
  return DetectionReportList.newBuilder()
      .addAllDetectionReports(
          matchedServices.stream()
              // The WordPressInstallPageDetector only works for web services.
              .filter(NetworkServiceUtils::isWebService)
              // Detection logic that checks whether a web service exposes
              // a wordpress installation page. Omitted here for simplicity.
              .filter(this::isServiceVulnerable)
              // Build a DetectionReport when the web service is vulnerable.
              .map(networkService -> buildDetectionReport(targetInfo, networkService))
              .collect(toImmutableList()))
      .build();
}

private DetectionReport buildDetectionReport(
    TargetInfo scannedTarget, NetworkService vulnerableNetworkService) {
  return DetectionReport.newBuilder()
      .setTargetInfo(scannedTarget)
      .setNetworkService(vulnerableNetworkService)
      .setDetectionTimestamp(Timestamps.fromMillis(Instant.now(utcClock).toEpochMilli()))
      .setDetectionStatus(DetectionStatus.VULNERABILITY_VERIFIED)
      .setVulnerability(
          Vulnerability.newBuilder()
              .setMainId(
                  VulnerabilityId.newBuilder()
                      .setPublisher("GOOGLE")
                      .setValue("UNFINISHED_WORD_PRESS_INSTALLATION"))
              .setSeverity(Severity.CRITICAL)
              .setTitle("Unfinished WordPress Installation")
              .setDescription(
                  "An unfinished WordPress installation exposes the /wp-admin/install.php page,"
                      + " which allows attacker to set the admin password and possibly"
                      + " compromise the system."))
      .build();
}
```

## 3. Preparing the `PluginBootstrapModule`

Each Tsunami plugin must have a companion `PluginBootstrapModule` that provides
the required Guice bindings and registers the plugin to the core engine.

Creating a `PluginBootstrampModule` is rather simple if you only need to
register the plugin. You only need to call `registerPlugin` within the
`configurePlugin` method (e.g.
[`ExampleVulnDetectorBootstrapModule`](https://github.com/google/tsunami-security-scanner-plugins/tree/master/examples/example_vuln_detector/src/main/java/com/google/tsunami/plugins/example/ExampleVulnDetectorBootstrapModule.java)).

A more complete example is the
[GenericWeakCredentialDetectorBootstrapModule](https://github.com/google/tsunami-security-scanner-plugins/blob/master/google/detectors/credentials/generic_weak_credential_detector/src/main/java/com/google/tsunami/plugins/detectors/credentials/genericweakcredentialdetector/GenericWeakCredentialDetectorBootstrapModule.java)
