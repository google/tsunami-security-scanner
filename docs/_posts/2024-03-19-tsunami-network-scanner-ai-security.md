---
authors:
- name: Annie Mao
excerpt: 'Interested in creating an AI-related plugin for the Tsunami network scanner and
getting rewarded for your efforts? See this post for details!'
title: 'Tsunami Network Scanner & AI Security'
---

You may already be familiar with the
[Tsunami Network Scanner](https://github.com/google/tsunami-security-scanner)
from our
[Patch Rewards program](https://bughunters.google.com/about/rules/4928084514701312/patch-rewards-program-rules#tsunami-patch-rewards),
which rewards external contributors for creating new
[detector plugins](https://github.com/google/tsunami-security-scanner-plugins/tree/master/google).
Now with AI being on everyone's minds, we want to double down on securing open
source AI infrastructure via Tsunami.

On our
[GitHub page](https://github.com/google/tsunami-security-scanner-plugins/issues),
you can find a list of AI-relevant **plugin & web fingerprint** implementation
requests tagged as "help wanted". **Anyone** can contribute to a Tsunami plugin
from this list, and the implementation will be reviewed & rewarded under our
Tsunami Patch Rewards program, with rewards ranging from $500 to $3,133.7
([details](https://bughunters.google.com/about/rules/4928084514701312/patch-rewards-program-rules#reward-amounts-tsunami-)).

Here are the rules of engagement for implementing AI-related plugins:

*   **First come, first served**: Each contributor can pick up any of the
    unassigned plugins, but please only take one **at a time**.
*   **Reassignment of inactive plugins**: If an assigned plugin has not been
    worked on for **over a week**, then the Tsunami review panel will unassign
    the contributor from the plugin. The plugin request is returned to the
    free-for-all pool.
*   **Vulnerability Research**: As a first step, the contributor has to provide
    detailed vulnerability research & an implementation design for the plugin to
    the review panel, and then wait for confirmation from the review panel
    before moving on to the implementation stage.
*   **Testbed Requirement**: All test containers or configurations for each
    plugin have to be submitted to
    [google/security-testbeds](https://github.com/google/security-testbeds).
*   **Review Priority**: If a contributor already has a different plugin in the
    review queue, we will prioritize reviewing the ML plugin, unless the
    originally provided plugin is critical.

Finally, we welcome you to propose new plugins that address critical security
issues in AI-serving frameworks and related tools on our
[GitHub page](https://github.com/google/tsunami-security-scanner-plugins/issues).
For faster acceptance, when sharing your proposal, please provide context on how
a given service is used in the AI ecosystem.

We're looking forward to collaborating with you to keep AI infrastructure
secure!
