# HTTP Lib Testdata

## tsunami_test_server.p12

This is a PKCS12 self-signed server key/cert file. This file was generated using
the following commands with the password `tsunamitest`:

```shell
$ openssl req -new -x509 -nodes -sha1 -days 3650 \
    -out /tmp/tsunami_test_server.crt \
    -keyout /tmp/tsunami_test_server.key
# Password is "tsunamitest" without the quotes.
$ openssl pkcs12 -export -clcerts \
    -in /tmp/tsunami_test_server.crt \
    -inkey /tmp/tsunami_test_server.key \
    -out tsunami_test_server.p12
```
