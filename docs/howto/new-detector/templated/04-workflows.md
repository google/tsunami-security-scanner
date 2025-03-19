
# Assembling the workflow

A workflow is simply a list of the actions to be executed, in order:

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

## What is next

[Using variables](05-variables)
