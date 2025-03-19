# Predefined variables

Tsunami will provide a predefined set of variable to the environment that you
can make use of in your actions. We try to maintain a strong naming convention
for these :

- `T_` stands for Tsunami and identifies a variable that is provided by the
core engine;
- `_UTL_` stands for utility and provides various utility variables;
- `_NS_` stands for network service and provide information about the currently
scanned network service;
- `_CBS_` stands for callback server and provides information about the
callback server.

Here is the list of variables that are provided:

- `T_UTL_CURRENT_TIMESTAMP_MS`: Provides the current timestamp in milliseconds.
Note that the timestamp is computed at the beginning of a workflow run. It will
thus be different between services but always return the same value within one
run;
- `T_NS_BASEURL`: The base URL of the network service being scanned. For example
`http://127.0.0.1:9090` or `http://hostname.lan:1000`;
- `T_NS_PROTOCOL`: The protocol used by the network service being scanned (e.g.
`tcp`);
- `T_NS_HOSTNAME`: The hostname of the network service being scanned. Note that
this variable is only available if Tsunami was invoked with a hostname target
(e.g. `hostname.lan`);
- `T_NS_PORT`: The port of the network service being scanned (e.g. `1000`);
- `T_NS_IP`: The IP of the network service being scanned (e.g. `127.0.0.1`);
- `T_CBS_URI`: The callback server URL used to trigger the callback server. This
is the main variable used when using the callback server. It contains the
address and hashed secret (e.g. `http://tsunami-callback.lan/8fe7d878787d65`
where `8fe7d878787d65` is the **hashed** secret);
- `T_CBS_SECRET`: The callback server secret generated for the current workflow
run; note that it is not hashed and is not relevant in most cases (e.g.
`somesecret`);
- `T_CBS_ADDRESS`: Address of the callback server (e.g. `tsunami-callback.lan`);
- `T_CBS_PORT`: Port of the callback server (e.g. `80`);
