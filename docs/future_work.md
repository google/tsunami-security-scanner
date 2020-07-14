# Future Work

## <a name="short_term"></a>Short Term

*   Adding more RCE plugins. See the
    [tsunami-security-scanner-plugins repo](https://github.com/google/tsunami-security-scanner-plugins).
*   Adding a web application fingerprinter for better plugin matching logic.

## <a name="long_term"></a>Long Term

### <a name="multi_lang_plugins"></a>Language Agnostic Plugins

More details to follow.

The main goal is to allow plugin authors to write Tsunami plugins in any
language they want. The design idea is to enable RPC communications between the
scanner and plugins. Similar implementation is NeoVim's plugin architecture.

### <a name="dynamic_orchestration"></a>Dynamic Scanning Orchestration

More details to follow.

Currently Tsunami follows a hard coded 2-step workflow. If we want to add more
steps or support more plugin types in the workflow, we have to make code changes
to the scanner.

The main goal of a Dynamic Scanning Orchestra is to allow users to simply drop
any type of plugins into the plugin pool and Tsunami will still be able to
execute it without modifying the scanner code.

How this could be implemented is still TBD, but the idea is to compile an
execution graph when the scanner starts, based on the input/output data
dependencies across all installed plugins.
