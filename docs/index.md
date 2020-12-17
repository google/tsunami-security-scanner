# Tsunami

## <a name="why_tsunami"></a>Why Tsunami?

When security vulnerabilities or misconfigurations are actively exploited by
attackers, organizations need to react quickly in order to protect potentially
vulnerable assets. As attackers increasingly invest in automation, the time
window to react to a newly released, high severity vulnerability is usually
measured in hours. This poses a significant challenge for large organizations
with thousands or even millions of internet-connected systems. In such
hyperscale environments, security vulnerabilities must be detected and ideally
remediated in a fully automated fashion. To do so, information security teams
need to have the ability to implement and roll out detectors for novel security
issues at scale in a very short amount of time. Furthermore, it is important
that the detection quality is consistently very high. To solve these challenges,
we created Tsunami - an extensible network scanning engine for detecting high
severity vulnerabilities with high confidence in an unauthenticated manner.

## <a name="goal"></a>Goals and Philosophy

*   Tsunami supports small manually curated set of vulnerabilities
*   Tsunami detects high severity, RCE-like vulnerabilities, which often
    actively exploited in the wild
*   Tsunami generates scan results with high confidence and minimal
    false-positive rate.
*   Tsunami detectors are easy to implement.
*   Tsunami is easy to scale, executes fast and scans non-intrusively.

## <a name="orchestration"></a>How Tsunami Scan Works

See [Tsunami Scan Orchestration](orchestration.md).

## <a name="howto"></a>How do I ...

*   ... [build and execute the scanner?](howto.md#build_n_execute)
*   ... [install Tsunami plugins?](howto.md#install_plugins)
*   ... [create a new Tsunami plugin?](howto.md#create_plugins)
*   ...
    [apply my plugins to certain types of services / software?](howto.md#filter_plugins)
*   ... [add command line arguments for my plugin?](howto.md#command_line)
*   ... [add configuration properties for my plugin?](howto.md#configuration)

## <a name="naming"></a>Naming

The name "Tsunami" comes from the fact that this scanner is meant be used as part of a larger system to warn owners about automated "attack waves". Automated attacks are similar to tsunamis in the way that they come suddenly, without prior warning and can cause a lot of damage to organizations if no precautions are taken. The term "Tsunami Early Warning System Security Scanning Engine" is quite long and thus the name got abbreviated to Tsunami Scanning Engine, or Tsunami. Hence, the name is not an analogy to tsunamis itself, but to a system that detects them and warns everyone about them.

## <a name="future_work"></a>Future Work

See [Future Work](future_work.md).
