# October update - Tsunami reward program

## Improving the PRP situation

Since our
[last update in June](https://google.github.io/tsunami-security-scanner/2025/06/18/changes-to-tsunami.html),
we have made good progress on merging incoming pull requests. Not only do we now
have a very low amount of requests to process, but most of them are now
implemented with the
[new templated language system](https://google.github.io/tsunami-security-scanner/howto/new-detector/templated/00-getting-started)
which is usually faster for us to merge.

**A big thank you to all of our contributors for their patience\!**

## An update on the payouts

Note:
[Our official rules](https://bughunters.google.com/about/rules/open-source/5067456626688000/tsunami-patch-rewards-program-rules)
have been updated accordingly.

We recently came to realize that our current payout system made the decision for
the reward difficult. To ensure everyone is rewarded fairly and adequately, we
have decided to simplify the payout system:

Type of detector                                     | Reward (up to dollars)
:--------------------------------------------------: | :--------------------:
Wishlist detector                                    | 3177.13
Exposed interface detector Weak credentials detector | 2000
Other detectors                                      | 1500

### What is a wishlist detector?

This is a detector for a vulnerability that Google cares deeply about. We
understand that this is outside of the control of the contributors but this is
generally based on internal priorities.

We will generally make it explicit that a contribution falls in that category
but on the other hand, we might request that the detector is completed in a
faster timeline (less than a week) to justify the higher payout. Sometimes we
will release a wishlist to the public – if you pick up an item from that
wishlist, you are guaranteed to fall into this category.

### What happened to fingerprints?

We are not accepting new fingerprinting contributions for now. **Note that pull
requests already opened will be processed and paid as previously agreed upon.**

We are currently working on completely changing the way Tsunami performs
fingerprinting. Amongst other things, we are experimenting with rewriting that
specific portion of the scanner in Golang to measure how well the language
matches our needs.

## An insight into our triage decisions

We also understand that it might be difficult to understand how and why we
decide to accept some contributions and not others, so we wanted to provide some
visibility into that process.

First and foremost, the goal of Tsunami is to find impactful vulnerabilities.
**This generally means that we want to identify security issues that have a
strong impact; this generally translates to remote code execution (RCE).**

**The questions that we are always asking ourselves:**

*   Can this be turned into a full-chain to remote code execution?
*   Can the full-chain be implemented in the detector? Or be reliable enough
    that it can ascertain the full chain exploitability?

Here is an example table for common vulnerability types:

| Category                 | Decision        | Additional information          |
| :----------------------: | :-------------: | :-----------------------------: |
| XSS                      | Rejected        |                                 |
| CSRF                     | Rejected        |                                 |
| SSRF                     | Likely rejected | Unless it can be instrumented   |
:                          :                 : to reach remote code execution. :
| SQLi                     | Likely rejected | Unless it can trivially be      |
:                          :                 : turned into an RCE (i.e. no     :
:                          :                 : dependency to a specific SQL    :
:                          :                 : backend)                        :
| Local file include       | It depends      | Unless the application provides |
:                          :                 : by default files that can be    :
:                          :                 : used to turn the include into   :
:                          :                 : an RCE.                         :
| Path traversal           | It depends      | Unless it can lead to RCE, for  |
:                          :                 : example with a secret file that :
:                          :                 : allows administrative access    :
:                          :                 : and remote code execution.      :
| XXE                      | It depends      | Unless it can be used to        |
:                          :                 : trigger RCE                     :
| Remote file include      | Likely accepted | Only if a remote file can be    |
:                          :                 : included in a way that results  :
:                          :                 : in remote code execution.       :
| File upload              | Likely accepted | Only if the upload allows       |
:                          :                 : remote code execution           :
| Exposed interface,       | Likely accepted | Only if the service that it     |
: authentication bypass or :                 : provides access to allows       :
: weak credentials         :                 : remote code execution           :
| OS command injection     | Likely accepted |                                 |

## Tsunami versioning

As we previously announced, we are slowly dropping Maven releases in favor of
our Docker images and direct dependencies to GitHub. We are already not
publishing any new artifacts to Maven and encourage you **strongly** to migrate
to building with the GitHub code.

This change slightly increases overall maintenance of plugins for larger changes
of the core but ensures that issues do not go unnoticed and also makes
dependencies management a lot easier for us.
