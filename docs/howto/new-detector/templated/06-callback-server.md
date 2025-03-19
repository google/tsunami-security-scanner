
# Using the callback server

If you wrote plugins for Tsunami before, you know about the callback server. If
you have not, the callback server is a mechanism that allows us to check for
vulnerabilities via an out-of-band mechanism. You can read more about it on the
[dedicated GitHub repository](https://github.com/google/tsunami-security-scanner-callback-server).

In a nutshell, the callback server works this way:

1. A secret is generated (and stored in `T_CBS_SECRET`);
2. This secret is hashed;
3. The exploit uses the hashed secret and the URL of the callback server
(`T_CBS_URI`) to trigger an out-of-band communication with the callback server;
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

{% raw %}
```proto
{ name: "payload" value: "%{ import os; os.system('curl {{ T_CBS_URI }}') }%" }
```
{% endraw %}

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

{% raw %}
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
{% endraw %}

Note: Because workflow are interpreted in order, the one that is more
restrictive needs to be defined first. Otherwise, the less restrictive workflow
would always be the one running.

## What is next

[Using cleanup actions](07-cleanup-actions)
