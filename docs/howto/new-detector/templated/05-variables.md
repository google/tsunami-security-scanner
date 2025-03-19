
# Using variables

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

{% raw %}
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
{% endraw %}

IMPORTANT: The syntax for variables is requires **exactly** one space before and
after the variable name, between the brackets. Otherwise, substitution will not
happen.

## Extracting information to local variables

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

## Predefined variables

Now would be a good time to look at the
[list of variables](glossary-predefined-variables) that Tsunami provides.

## What is next

[Using the callback server](06-callback-server)
