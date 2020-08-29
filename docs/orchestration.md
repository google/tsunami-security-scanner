# Tsunami Scan Orchestration

## Overview

As of today, Tsunami follows a hardcoded 2-step process when scanning a publicly
exposed network endpoint (see
[Future Work](future_work.md#dynamic_orchestration) on the potential
improvement on the workflow):

*   **Reconnaissance**: In the first step, Tsunami identifies open ports and
    subsequently fingerprints protocols, services and other software running on
    the target host via a set of fingerprinting plugins. To not reinvent the
    wheel, Tsunami leverages existing tools such as [nmap](https://nmap.org/)
    for some of these tasks.
*   **Vulnerability verification**: Based on the information gathered in step 1,
    Tsunami selects all vulnerability verification plugins matching the
    identified services and executes them in order to verify vulnerabilities
    without false positives.

## Overall Scanning Workflow

Following diagram shows the overall workflow for a Tsunami scan.

![orchestration](img/orchestration.svg)

## Reconnaissance

In the reconnaissance step, Tsunami probes the scan target and gathers as much
information about the scan target as possible, including:

*   open ports,
*   protocols,
*   network services & their banners,
*   potential software & corresponding version.

Tsunami performs the Reconnaissance step in 2 separate phases.

### Port Scanning Phase

In the port scanning phase, Tsunami performs port sweeping in order to identify
open ports, protocols and network services on the scan target. The output of
Port Scanning is a `PortScanReport` protobuf that contains all the identified
`NetworkService`s from the port scanner.

`PortScanner` is a special type of Tsunami plugins design for Port Scanning
purpose. This allows users to swap the port scanning implementations. To not
reinvent the wheel, users could choose a Tsunami plugin wrapper around existing
tools like [nmap](https://nmap.org/) or
[masscan](https://github.com/robertdavidgraham/masscan). You may find useful
`PortScanner` implementations can be found in
[tsunami-security-scanner-plugins](https://github.com/google/tsunami-security-scanner-plugins)
repo.

### Fingerprinting Phase

Usually port scanners only provide very basic service detection capability. When
the scan target hosts complicated network services, like web servers, the
scanner needs to perform further fingerprinting work to learn more about the
exposed network services.

For example, the scan target might choose to serve multiple web applications on
the same TCP port 443 using nginx for reverse proxy, `/blog` for WordPress, and
`/forum` for phpBB, etc. Port scanner will only be able to tell port 443 is
running nginx. A Web Application Fingerprinter with a comprehensive crawler is
required to identify these applications.

`ServiceFingerprinter` is a special type of Tsunami plugin that allows users to
define fingerprinters for a specific network service. By using filtering
annotations (see
[how to apply my plugins to certain types of services / software?](howto.md#filter_plugins)),
Tsunami will be able to automatically invoke appropriate `ServiceFingerprinter`s
when it identifies matching network services.

### Reconnaissance Report

At the end of the reconnaissance step, Tsunami compiles both the port scanner
outputs and service fingerprinter outputs into a single `ReconnaissanceReport`
protobuf for Vulnerability Verification.

## Vulnerability Verification

In the Vulnerability Verification step, Tsunami executes the `VulnDetector`
plugins in parallel to verify certain vulnerabilities on the scan target based
on the information gathered in the Reconnaissance step. `VulnDetector`'s
detection logic could either be implemented as plain Java code, or as a separate
binary / script using a different language like python or go. External binaries
and scripts have to be executed as separate processes outside of Tsunami using
Tsunami's command execution util. See
[Future Work](future_work.md#multi_lang_plugins) for our design ideas of
making Tsunami plugins language agnostic.

### Detector Selection

Usually one `VulnDetector` only verifies one vulnerability and the vulnerability
often only affects one type of network service or software. In order to avoid
doing wasteful work, Tsunami allows plugins to be annotated by some filtering
annotations (see [how-to guide](howto.md#filter_plugins) for details) to limit
the scope of the plugin.

Then before the Vulnerability Verification step starts, Tsunami will select
matching `VulnDetector`s to run based on the exposed network services and
running software on the scan target. Non-matching `VulnDetector`s will stay
inactive throughout the entire scan.
