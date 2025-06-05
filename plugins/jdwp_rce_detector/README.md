# JDWP RCE Detector Plugin

This plugin for the Tsunami Security Scanner detects potential Remote Code Execution (RCE) vulnerabilities exposed via the Java Debug Wire Protocol (JDWP).

JDWP is a protocol used for debugging Java applications. If JDWP is exposed unintentionally in a production environment and not properly secured, it can allow attackers to execute arbitrary code on the server running the Java application.

This plugin identifies network services with open JDWP ports and reports them as potential vulnerabilities.

## Vulnerability Details

- **Severity**: Critical
- **Description**: The Java Debug Wire Protocol (JDWP) is enabled on a publicly accessible port. A remote attacker can connect to this port and execute arbitrary Java code within the context of the application, leading to Remote Code Execution (RCE).
- **Recommendation**:
    - Disable JDWP in production environments.
    - If JDWP is required for specific purposes, ensure it is not exposed to untrusted networks. Use firewall rules to restrict access to the JDWP port to only authorized IP addresses or VPN connections.
    - Configure JDWP to listen on a local interface only (e.g., `127.0.0.1`) if remote debugging is not strictly necessary.

## Plugin Information

- **Plugin Name**: JdwpRceDetector (or similar, will be updated in the `PluginInfo` annotation)
- **Version**: 0.1
- **Author**: Your Name/Organization
- **Type**: VULN_DETECTION
