# Tsunami Payload Generation Framework

This is the code for Tsunami's payload generation framework, an optional library
for detectors which automatically selects the best payload for a given
vulnerability, taking out the guesswork when writing a new detector, reducing
false positives, and standardizing payloads across detectors. It is also the
interface for using the
[Tsunami Callback Server](https://github.com/google/tsunami-security-scanner-callback-server).

Detectors targeting remote code executions (RCE) and server-side request forgery
(SSRF) vulnerabilities are ideal candidates for using the payload framework.

For an example of how to use the framework, see
[the example plugin](https://github.com/google/tsunami-security-scanner-plugins/tree/master/examples/example_payload_framework_vuln_detector).

## payload_definitions.yaml

[payload_definitions.yaml](https://github.com/google/tsunami-security-scanner/blob/master/plugin/src/main/resources/com/google/tsunami/plugin/payload/payload_definitions.yaml)
defines the actual payloads used in the payload generation framework. See the
schema definition in
[payload_generator.proto](https://github.com/google/tsunami-security-scanner/blob/master/proto/payload_generator.proto).
When adding a new payload definition, make sure to add
[test cases](https://github.com/google/tsunami-security-scanner/blob/master/plugin/src/test/java/com/google/tsunami/plugin/payload/PayloadGeneratorTest.java).
