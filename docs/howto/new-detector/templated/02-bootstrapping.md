
# Bootstrapping the plugin

*This section assumes that you already know how to
[build and run Tsunami](https://google.github.io/tsunami-security-scanner/howto/howto).*

## Before starting

Before we start, we encourage you to take a quick look at the setup guide for
the linter of the configuration language. Your plugin will be expected to pass
all the checks in this linter. Warnings will have to be justified before being
merged as well.

[See the instructions for the linter](appendix-using-linter)

## Our vulnerable application

For this tutorial series, we will write a simple detector for a non-existing
vulnerability `CVE-0123-12345`. We want our detector to:

- *Fingerprint the application*: we only want to send other requests if we are
sure that we are dealing with the potentially vulnerable application; So we are
going to send an HTTP request to `/version` and check for the presence of the
banner `MyVulnerableApp` in the `Server` header;

- *Exploit the application*: once we are sure that we are dealing with the right
application, we are going to send a `POST` request to `/exploit` with a custom
payload `%{ print("tsunami_%d_marker", 1250*1+3) }%` in the `process` field.
Then we are going to verify that it gets executed by verifying that the answer
contains `tsunami_1253_marker`;

*Note: In this example, we simulate a template injection vulnerability that is
going to cause `%{ print("tsunami_%d_marker", 1250*1+3) }%` to be interpreted.
The vulnerable language or the syntax have no importance. The result of
`1250*1+3` is going to be replaced at the `%d` placeholder, resulting in
`tsunami_1253_marker` being printed in the response.*

## Providing information about our plugin

Let's create a new file in our local copy of the
[Tsunami plugin repository](https://github.com/google/tsunami-security-scanner-plugins)
as `templated/templateddetector/plugins/cve/0123/MyVulnerableApp_CVE_0123_12345.textproto`.

Let's add the header that will help linters to understand which proto definition
we are using:

```proto
# proto-file: third_party/tsunami_plugins/templated/templateddetector/proto/templated_plugin.proto
# proto-message: TemplatedPlugin
```

Then we provide basic information about our plugin. Note that the name of the
plugin is used to **uniquely identify it and thus should be unique across all
plugins**.

```proto
info: {
  type: VULN_DETECTION
  name: "MyVulnerableApp_CVE_0123_12345"
  author: "Some developper <myemail@provider.com>"
  version: "1.0"
}
```

Now is a good time to read about our
[naming conventions for plugins](appendix-naming-plugin).

## Information about the vulnerability

Let's add information about the vulnerability itself. These are the information
that will be used to generate the vulnerability report and notify about the
vulnerability.

Important: whenever a CVE is associated with the vulnerability the `related_id`
field must be filled with the associated CVE reference.

```proto
finding: {
  main_id: {
    publisher: "GOOGLE"
    value: "SOME_PLUGIN_DETECTION"
  }
  title: "Example templated plugin"
  description: "This is an example templated plugin."
  recommendation: "No recommendation, this is an example."
  related_id: {
    publisher: "CVE"
    value: "CVE-0123-12345"
  }
}
```

## Configuring the plugin

The next section in our file, is the `config` section. It allows some basic
tuning of the plugin. For now, let's keep it empty:

```proto
config: {}
```

But it is important to keep in mind that this section allows:

- To enable `debug` mode. In debug mode, the plugin will generate highly verbose
debugging messages. For example, it will log every HTTP request and response;
- To switch the plugin to be `disabled`. Note that even when disabled (unless
explicitly specified in the test file) the tests for that plugin will still be
executed.

If we were to run our plugin now, we would see the plugin being registered but
we would get a non-blocking error at runtime because we have no workflow or
action defined:

```sh
{ ... }
Dec 31, 2024 11:07:58 AM com.google.tsunami.plugin.PluginBootstrapModule registerDynamicPlugin
INFO: Dynamic plugin registered: MyVulnerableApp_CVE_0123_12345
{ ... }
Dec 31, 2024 11:08:15 AM com.google.tsunami.plugins.detectors.templateddetector.TemplatedDetector detect
INFO: Starting detector: MyVulnerableApp_CVE_0123_12345
Dec 31, 2024 11:08:15 AM com.google.tsunami.plugins.detectors.templateddetector.TemplatedDetector detect
SEVERE: No workflow matched the current setup. Is plugin 'MyVulnerableApp_CVE_0123_12345' misconfigured?
{ ... }
```

## What is next

[Writing the first actions](03-first-actions)
