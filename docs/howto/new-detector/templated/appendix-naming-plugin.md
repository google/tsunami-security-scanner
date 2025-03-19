# Convention: How to name a plugin

The plugin name and filename should be identical as it makes for easier
discoverability. Plugins should be named using the following convention:

- All plugins should be named using the following character set: `[a-zA-Z0-9_]`
so no spaces or special characters.
- If the vulnerability has an associated CVE:
`VulnerableApplicationName_CVE_YYYY_NNNNN` and the plugin should be placed in
the `cve/YYYY/` directory.
- If the vulnerability does not have an associated CVE:
`VulnerableApplicationName_YYYY_VulnerabilityName`; if a vulnerability has no
name you can try to describe it, for example `PreauthRCE`. The vulnerability
will then be placed in the directory that matches the type of vulnerability,
for example `rce/YYYY/VulnerableApplicationName_YYYY_VulnerabilityName`.
- When the name of a plugin contains an acronym (e.g. `HTTP`, `UI`, `RCE`),
that acronym must be in uppercase.
