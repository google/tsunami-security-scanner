# Templated plugins documentation

## Introduction

### What is a templated plugin?

In the past, if you wanted to write a Tsunami detector, you would need to
implement your detector using Java or Python. For each, you would have to write
a set of tests and ensure that everything is compiling and working as intended.

This process proved to be very time consuming; especially as most Tsunami
detectors are simply sending an HTTP request and checking the response code and
body content. That is why we introduced templated plugins.

### How does it work?

We have abstracted most of the code required to write a plugin. All you need to
do is to write a `.textproto` file that describes the behavior of your
plugin and a `_test.textproto` file that describes the tests for the plugin.

A `.textproto` is a human-readable text representation of a protocol buffer
message. If you are not familiar with protocol buffers, we recommend checking
the [official documentation](https://protobuf.dev/), but for our use case, you
can think of it as a strongly typed JSON or YAML.

The `.textproto` files are compiled into binary format and embedded as resources
to a meta plugin that we will refer to as the templated Tsunami plugin. At
runtime, the templated plugin will interpret the behavior described in each file
and dynamically create a new detector for it.

![High-level-overview](../img/templated-how-it-works.png)

### How do I know how to write a templated plugin?

We have tried to cover as much as possible in this documentation. But the
configuration language is bound to evolve with time. If you have any doubt, the
source of truth will always be the
[proto definition](https://github.com/google/tsunami-security-scanner-plugins/templated/templateddetector/proto/)
which we aim at keeping as straightforward and commented as possible.

### Execution workflow of a templated plugin

Each plugin is defined by a set of two high-level concepts: **Actions** and
**workflows**.

- **Actions** are the basic unit of execution in a templated plugin. They are
responsible for performing one specific... well... action and returning a
boolean value that defines whether it was successful. An example action, in
plain English, could be:

> Send an HTTP request to the target and verify that the returned status code
> is 200

- Once you have defined a set of actions, you need to be able to define an order
in which they will be executed: this is the role of **workflows**.

To write a plugin, we need to define a set of actions and put them in a
specific order with workflows. But how are things executed? The engine processes
plugins in the following way:

1. Extracts all the workflows and actions defined in the plugin;
2. Performs very basic checks to ensure that everything is well defined;
3. Goes through all the defined workflows and execute the first that matches
the conditions it was defined with;
4. Executes every action of the workflow in order until one fails or all actions
are successful;
5. If any action failed, there is no vulnerability. If all actions were
successful, the vulnerability is present.

Steps 4 and 5 are repeated for every network service found during the port
scanning phase of Tsunami. We call steps 4 and 5 a **run of the workflow**.

## Getting started

Now that we have a better grasp of how templated plugins work, we can try to
write our first plugin.

*This section assumes that you already know how to
[build and run Tsunami](https://google.github.io/tsunami-security-scanner/howto/howto).*

### Our vulnerable application

For this basics series, we will write a simple detector for a non-existing
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

### How to name a plugin

A note about naming plugins: The plugin name and filename should be
identical as it makes for easier discoverability. Plugins should be named using
the following convention:

- All plugins should be named using the following character set: `[a-zA-Z0-9_]`
- If the vulnerability has an associated CVE:
`VulnerableApplicationName_CVE_YYYY_NNNNN` and the plugin should be placed in
the `cve/YYYY/` directory.
- If the vulnerability does not have an associated CVE: `VulnerableApplicationName_YYYY_VulnerabilityName`; if a vulnerability has no
name you can try to describe it, for example `PreauthRCE`. The vulnerability
will then be placed in the directory that matches the type of vulnerability,
for example `rce/YYYY/VulnerableApplicationName_YYYY_VulnerabilityName`.


### Providing information about our plugin and the vulnerability

Let's create a new file in our local copy of the
[Tsunami plugin repository](https://github.com/google/tsunami-security-scanner-plugins)
as `templated/templateddetector/plugins/cve/0123/MyVulnerableApp_CVE_0123_12345.textproto`.

Let's add the header that will help linters to understand which proto definition
we are using:

```proto
# proto-file: third_party/tsunami_plugins/templated/templateddetector/proto/templated_plugin.proto
# proto-message: TemplatedPlugin
```

Then let's provide basic information about our plugin. Note that the name of the
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

Let's add information about the vulnerability itself. These are the information
that will be used to generate the vulnerability report and notify about the
vulnerability.

Whenever possible, the `related_id` field must be filled with the associated CVE
reference.

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

### Configuring the plugin

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
we would get a non-blocking error at runtime because we have no workflow and
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

### Writing the first actions

Each action is defined by a name and a subtype. A subtype defines what the
action is able to do: For example "HTTP request" is a subtype that allows to
send an HTTP request and inspect responses.

Let's start building our fingerprinting step with a very simple action:

```proto
actions: {
  name: "fingerprinting"
  http_request: {
    method: GET
    uri: "/version"
  }
}
```

In that action, named `fingerprinting` we simply send a `GET` HTTP request to
`/version` on the currently targeted service.

*Note that Tsunami will only send HTTP requests to services that have been
identified as HTTP services during the port scanning phase.*

But that action will always succeed because it does not verify anything in the
response. Let's use the `response` field and add a condition on the status code:

```proto
actions: {
  name: "fingerprinting"
  http_request: {
    method: GET
    uri: "/version"
    response: {
      http_status: 200
    }
  }
}
```

Now we want to ensure the header contains `MyVulnerableApp`: we need to use
**expectations**. We can either use `expect_all` or `expect_any`, which perform
checks on the response that must respectively "all be true" or
"at least one true". Here, we have only one expectation, so we will use
`expect_all` with one `conditions`:

```proto
actions: {
  name: "fingerprinting"
  http_request: {
    method: GET
    uri: "/version"
    response: {
      http_status: 200
      expect_all: {
        conditions: [
          { header: { name: "Server" } contains: "MyVulnerableApp" }
        ]
      }
    }
  }
}
```

The condition is that the `header` with `name` `Server` `contains` the string
`MyVulnerableApp`.

Now, let's build a `POST` request for our exploitation step:

```proto
actions: {
  name: "exploitation"
  http_request: {
    method: POST
    uri: "/exploit"
    data: "process=%{ print(\"tsunami_%d_marker\", 1250*1+3) }%"
    response: {
      http_status: 200
      expect_all: {
        conditions: [
          { body: {} contains: "tsunami_1253_marker" }
        ]
      }
    }
  }
}
```

This action is very similar to the previous one but:

- It sends a `POST` request instead of a `GET` one;
- As part of the `POST` it sends `process=%{ print(\"tsunami_%d_marker\", 1250*1+3) }%`
as data;
- The expectation has been changed to check that the response body
contains `tsunami_1253_marker`.

### Naming actions

Actions must be named using the `[a-zA-Z0-9_]` character set. For example
`this_is_my_action`. This naming convention helps improving discoverability of
actions.

### Assembling the workflow

```proto
workflows: {
  actions: [
    "fingerprinting",
    "exploitation"
  ]
}
```

And that's it! We have written our first templated plugin. What happens if we
run it against a default HTTP server?

```sh
{ ... }
Dec 31, 2024 11:29:58 AM com.google.tsunami.plugin.PluginBootstrapModule registerDynamicPlugin
INFO: Dynamic plugin registered: MyVulnerableApp_CVE_0123_12345
{ ... }
Dec 31, 2024 11:30:15 AM com.google.tsunami.plugins.detectors.templateddetector.TemplatedDetector detect
INFO: Starting detector: MyVulnerableApp_CVE_0123_12345
Dec 31, 2024 11:30:15 AM com.google.tsunami.common.net.http.OkHttpHttpClient send
INFO: Sending HTTP 'GET' request to 'http://127.0.0.1:9090/version'.
Dec 31, 2024 11:30:15 AM com.google.tsunami.common.net.http.OkHttpHttpClient parseResponse
INFO: Received HTTP response with code '404' for request to 'http://127.0.0.1:9090/version'.
Dec 31, 2024 11:30:15 AM com.google.tsunami.plugins.detectors.templateddetector.TemplatedDetector runWorkflowForService
INFO: No vulnerability found because action 'Fingerprint' failed.
Dec 31, 2024 11:30:15 AM com.google.tsunami.plugin.PluginExecutorImpl buildSucceededResult
INFO: MyVulnerableApp_CVE_0123_12345 plugin execution finished in 119 (ms)
{ ... }
```

As expected, a default HTTP server does not have a `Server` header that contains
`MyVulnerableApp`.

### Using variables

In our action, we have hardcoded our payload, which is not super convenient and
readable:

```proto
actions: {
  name: "exploitation"
  http_request: {
    method: POST
    uri: "/exploit"
    data: "process=%{ print(\"tsunami_%d_marker\", 1250*1+3) }%"
    response: {
      http_status: 200
      expect_all: {
        conditions: [
          { body: {} contains: "tsunami_1253_marker" }
        ]
      }
    }
  }
}
```

Let's try to move our payload into a variable. For this, we will define
variables in our workflow:

```proto
workflows: {
  variables: [
    { name: "payload" value: "%{ print(\"tsunami_%d_marker\", 1250*1+3) }%" },
    { name: "payload_result" value: "tsunami_1253_marker" }
  ]

  actions: [
    "fingerprinting",
    "exploitation"
  ]
}
```

Which can then be used in the action:

```proto
actions: {
  name: "exploitation"
  http_request: {
    method: POST
    uri: "/exploit"
    data: "process={{ payload }}"
    response: {
      http_status: 200
      expect_all: {
        conditions: [
          { body: {} contains: "{{ payload_result }}" }
        ]
      }
    }
  }
}
```

IMPORTANT: The syntax for variables is requires **exactly** one space before and
after the variable name, between the brackets. Otherwise, substitution will not
happen.

### Extracting information to local variables

Expectations are sufficient if we need to check that the response has a very
specific content. But what if we need to extract some information from the
response for later use? For example, what if we have an action that creates a
job and we need the job name in a follow-up action to trigger the vulnerability?

In that case we can use **extractions** and **local variables**. But what are
local variables? There are two types of variables:

- **Workflow variables**: Defined at the workflow level they mostly define
static content. They will exist for the whole lifecycle of the workflow and will
be reset to their defined value between workflow runs. Note that workflow
variables are technically mutable, but we strongly recommend not mutating
them (i.e. not using them in extractions). We used this type of variable in the
previous example.
- **Local variables**: These variables are more dynamic. They are defined from
**extractions** and are only valid for the current workflow run. They are mostly
used to extract information that will be used in a later action.

For our previous example, let us assume that the `/version` page returns a CSRF
token that we will need to use in the `/exploit` request. We can use a local
variable to store that value:

```proto
actions: {
  name: "fingerprinting"
  http_request: {
    method: GET
    uri: "/version"
    response: {
      http_status: 200
      expect_all: {
        conditions: [
          { header: { name: "Server" } contains: "MyVulnerableApp" }
        ]
      }
      extract_all: {
        patterns: [
          {
            from_body: {}
            regexp: "CSRFToken=([a-zA-Z0-9_]+)"
            variable_name: "csrf_token"
          },
        ]
      }
    }
  }
}
```

In that example, we:

- still verify that the server has the `Server` header that we expect;
- also try to `extract_all` `patterns` `from_body` given regular expression
`regexp` and store the result in the variable `variable_name` (here
"csrf_token").

The extraction system offers **exactly** one capture group and extracting
several information should be set into different patterns.

WARNING: The use of extractions with `extract_any` should be done carefully. We
very strongly recommend to only use `extract_any` with the
**same variable name** between extractions. Doing otherwise makes the plugin
potentially flaky and difficult to debug.

### Predefined variables

Tsunami will provide a predefined set of variable to the environment that you
can make use of in your actions. We try to maintain a strong naming convention
for these :

- `T_` stands for Tsunami and identifies a variable that is provided by the
core engine;
- `_NS_` stands for network service and provide information about the currently
scanned network service;
- `_CBS_` stands for callback server and provides information about the
callback server.

Here is the list of variables that are provided:

- `T_NS_BASEURL`: The base URL of the network service being scanned. For example
`http://127.0.0.1:9090` or `http://hostname:1000`;
- `T_NS_PROTOCOL`: The protocol used by the network service being scanned (e.g.
`tcp`);
- `T_NS_HOSTNAME`: The hostname of the network service being scanned. Note that
this variable is only available if Tsunami was invoked with a hostname target;
- `T_NS_PORT`: The port of the network service being scanned;
- `T_NS_IP`: The IP of the network service being scanned;
- `T_CBS_URI`: The callback server URL used to trigger the callback server;
- `T_CBS_SECRET`: The callback server secret generated for the current workflow run;
- `T_CBS_ADDRESS`: Address of the callback server;
- `T_CBS_PORT`: Port of the callback server;

### Using the callback server

If you wrote plugins for Tsunami before, you know about the callback server. If
you have not, the callback server is a mechanism that allows us to check for
vulnerabilities via an out-of-band mechanism. You can read more about it on the
[dedicated GitHub repository](https://github.com/google/tsunami-security-scanner-callback-server).

In a nutshell, the callback server works this way:

1. A secret is generated;
2. This secret is hashed;
3. The exploit uses the hashed secret and the URL of the callback server to
trigger an out-of-band communication with the callback server;
4. The secret can be used to ask the callback server if a communication using
the hashed secret has been logged (i.e. if the vulnerability has been
triggered);

Currently, the templated plugin system does not support payload generation,
so communication with the callback server has to be handled semi-manually.

Every workflow run will automatically generate a new secret, its hash and the
adequate trigger URL. That means that, as part of your exploit, you will need to
make sure a request to the callback server is made with the trigger URL. The
trigger URL is stored in the `T_CBS_URI` variable. For example, in our previous
example we could change the payload to:

```proto
{ name: "payload" value: "%{ import os; os.system('curl {{ T_CBS_URI }}') }%" }
```

`T_CBS_URI` would be replaced with the trigger URL and the callback server
would receive a request on that endpoint if the vulnerability is triggered.
Checking if the vulnerability was triggered then simply requires defining a new
action:

```proto
actions: {
  name: "check_callback_server_logs"
  callback_server: { action_type: CHECK }
}
```

And, voila! But wait... How do we know, in our plugin, if the callback server
is currently running? Do we not want to support both use cases? That is one of
the features offered by workflows: `condition` allows you to define a condition
for the workflow to be executed. With our example:

```proto
# Workflow that uses the callback server.
workflows: {
  condition: REQUIRES_CALLBACK_SERVER
  variables: [
    { name: "payload" value: "%{ import os; os.system('curl {{ T_CBS_URI }}') }%" }
    ## note: empty string is always present in the body. This cancels out the
    ## body content expectation.
    { name: "payload_result" value: "" }
  ]

  actions: [
    "fingerprinting",
    "exploitation",
    "check_callback_server_logs"
  ]
}

# Workflow that does not use the callback server.
workflows: {
  variables: [
    { name: "payload" value: "%{ print(\"tsunami_%d_marker\", 1250*1+3) }%" },
    { name: "payload_result" value: "tsunami_1253_marker" }
  ]

  actions: [
    "fingerprinting",
    "exploitation"
  ]
}
```

Note: Because workflow are interpreted in order, the one that is more
restrictive needs to be defined first. Otherwise, the less restrictive workflow
would always be the one running.
