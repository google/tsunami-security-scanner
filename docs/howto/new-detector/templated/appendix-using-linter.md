
# Using the linter

For all plugins written using our configuration format, we expect the plugins to
be linted.

The linter ensures that the plugin has the right format but also performs a
series of checks that make sure it is behaving correctly.

## Installing the linter

### Using our docker image

The linter is bundled in our docker image and will automatically run.

### Custom setup

The linter is a Go binary that can very easily be installed:

```
$ go install github.com/google/tsunami-security-scanner-plugins/templated/utils/linter@latest
$ linter <path to the file>
```

Note that depending on your current configuration, you might have to extend your
`PATH`. See the
[Golang documentation](https://go.dev/doc/tutorial/compile-install) for details.
