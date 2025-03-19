## Magic tests URIs

The following URIs are considered "magic" in tests when using the mock HTTP
server:

- `TSUNAMI_MAGIC_ANY_URI`: Will match any URI; so this answer will match any
request;
- `TSUNAMI_MAGIC_ECHO_SERVER`: Force Tsunami to repeat the request in the
response. This is used internally to detect flaky detectors;
