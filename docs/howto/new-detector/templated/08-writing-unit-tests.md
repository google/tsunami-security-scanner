
# Writing unit tests

For every workflow, we expect to see associated unit tests for each of your
contributions. If unit tests are not as reliable as integration testing,
especially in the context of the scanner, they provide some insurance that
changes are not breaking detectors.

## Configuration of the unit test

The tests for a specific plugin are defined in a test file that is named exactly
like the detector, with the `_test` suffix. For example, for a
`NodeRED_ExposedUI.textproto` you can find its
`NodeRED_ExposedUI_test.textproto` counterpart.

Once created, the file needs to contain a minimal configuration:

```proto
config: {
  tested_plugin: "nameOfTheTestedPlugin"
}
```

Where `nameOfTheTestedPlugin` is the name of the plugin. The `tested_plugin`
configuration indicates to the test engine which detector to bind the test to.
Without this option, your test will not work.

You can additionally define the `disabled` configuration option, but in most
cases you will not need to use it.

## Defining tests

Once you have configured the general option for your unit tests, you need to
actually define each tests.

Most test will rely on a mock server to simulate the target application that
we wrote a detector for. But before we dig deeper into mock servers, each
test needs to have a name and whether the vulnerability will be found, for
example:

```proto
tests: {
  name: "whenVulnerable_returnsTrue"
  expect_vulnerability: true
}

tests: {
  name: "whenNotVulnerable_returnsFalse"
  expect_vulnerability: false
}
```

## Using mock servers

Now we can start simulating the behavior of our vulnerable application. Two mock
capabilities are currently integrated in the templated plugin system:

- An HTTP server;
- A fake Callback server;

Several mocks can be used at the same time.

Let us take a simplified version of our previously defined plugin:

```proto
actions: {
  name: "exploitation"
  http_request: {
    method: POST
    uri: "/exploit"
    data: "process=%{ import os; os.system('curl {{ T_CBS_URI }}') }%"
    response: {
      http_status: 200
    }
  }
}

actions: {
  name: "check_callback_server_logs"
  callback_server: { action_type: CHECK }
}

workflows: {
  condition: REQUIRES_CALLBACK_SERVER
  actions: [
    "exploitation",
    "check_callback_server_logs"
  ]
}
```

To validate vulnerability detection of this plugin, we will need:

- To simulate the callback server to return true. This can easily be done with
the `mock_callback_server` directive;
- To simulate the answer to `/exploit` with the HTTP server which can easily be
done with the `mock_http_server` directive;

```proto
tests: {
  name: "whenVulnerable_returnsTrue"
  expect_vulnerability: true

  mock_callback_server: {
    enabled: true
    has_interaction: true
  }

  mock_http_server: {
    mock_responses: [
      {
        uri: "/exploit"
        status: 200
      },
    ]
  }
}
```

And that is it. If we wanted to check a case where the server is not vulnerable
we could use the following test case:

```proto
tests: {
  name: "whenNotVulnerable_returnFalse"
  expect_vulnerability: false

  mock_http_server: {
    mock_responses: [
      {
        uri: "/exploit"
        status: 403
      },
    ]
  }
}
```

Note that we do not need the callback server anymore as the workflow will fail
before. Additionally, the `/exploit` endpoint now returns a `403`.

## Adding a bit of magic to our world

When mocking HTTP responses, Tsunami provides a few magic endpoints (to be
used in the `uri` field).

You can view them in the [glossary](glossary-tests-magic-uri).

## Generating things for you

Finally, under the hood, Tsunami will also generate unit tests for you. This
helps us detecting flaky detectors: for example when a detector fail to pass the
"echo server" test (when the server is just repeating the request, a detector
should not raise a vulnerability).

## Congratulations!

Congratulations, you have finished writing your first templated plugin!
