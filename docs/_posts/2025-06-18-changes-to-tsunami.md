---
authors:

- name: Pierre Precourt
excerpt: ‘Templated plugins are now the default for writing plugins and making the reward program more efficient.'
title: 'Changes to Tsunami'
---

# Changes to Tsunami

## Improving the situation on the Patch Reward Program (PRP)

Whether you have been a long time contributor or a newcomer to Tsunami, you
might have noticed that it takes a long time before a contribution is merged. We
thought it might be valuable to provide some context into why this happens:

-   First and foremost, the Tsunami team is a rather small team (1-2 people)
    with varying priorities.
-   Even though we are working with partners to reduce the time to review
    contributions, it still takes us a lot of time to merge contributions. That
    is because the external version of Tsunami and the one we are using
    internally are slightly different, most notably their build systems.

Now, this does not mean that we should not strive to make the situation better.
One bet that we are taking is templated plugins (introduced later in this
article). Without going into details, this new type of plugin should abstract
the differences between the two build systems, in a way that should make merging
plugins much easier for us and, thus, much faster for contributors.

## Tsunami templated plugins

To cite
[our official documentation](https://google.github.io/tsunami-security-scanner/howto/new-detector/templated/00-getting-started):

> In the past, if you wanted to write a Tsunami detector, you would need to
> implement your detector using Java or Python. For each, you would have to
> write a set of tests and ensure that everything is compiling and working as
> intended.

> This process proved to be very time consuming; especially as most Tsunami
> detectors are simply sending an HTTP request and checking the response code
> and body content. That is why we introduced templated plugins.

> We have abstracted most of the code required to write a plugin. All you need
> to do is to write a .textproto file that describes the behavior of your plugin
> and a _test.textproto file that describes the tests for the plugin.

In short, templated plugins allow contributors to write Tsunami plugins as if
they were configuration files.

### Why this change?

First and foremost, this makes writing Tsunami plugins much easier. It reduces
adherence to the build system and to the language, which makes it accessible to
more contributors. Because the plugins are now in a structured format, we can
also perform analysis on detectors and find common mistakes in these detectors:
this should overall improve the quality of plugins.

But that is only from the contributors’ perspective. From a maintainer
perspective, that also means that we can work more efficiently on changes at
scale in the Tsunami engine. Currently, whenever we need to make a change to the
engine, we often have to change about 100 plugins.

Finally, we are having very active discussions internally to rewrite Tsunami
entirely in Golang. If we were to decide to take this path, templated plugins
would help us make the migration easier.

### Why not YAML?

The most frequently asked question about templated plugins is: Why not use YAML
instead of textproto? First, we believe that
[YAML has a lot of issues](https://ruudvanasseldonk.com/2023/01/11/the-yaml-document-from-hell).

But the most important reason is that textprotos are checked at compilation time
and enforce a strong structure to our plugins (on top of being smaller).

### How does this affect the Patch Reward Program (PRP)?

Templated plugins are now the default for writing plugins. **We will stop
accepting Java and Python plugins unless there is a good reason for it (we
understand that templated plugins can be limiting in some use cases)**.

Other than that, no big changes. The rewards will stay the same and so will the
queue system. If our bet shows to be successful and templated plugins really
reduce the time for plugins to be merged, we plan to increase the contributor
queue as well. This would mean that a contributor could work on more plugins in
parallel. Stay tuned.

### Will older plugins be rewritten?

Most likely. We have not yet come-up with a detailed plan on how to do it, but
we would like to rewrite as many plugins as possible to unify them.

## Tsunami releases

## Tsunami version 1.0.0

For a long time, Tsunami has been in Alpha release. Internally, we have been
using Tsunami consistently and at scale for a while now. Thus we believe that
Tsunami is ready to be officially released in version 1.0.0.

### Maven releases

For now, we are releasing most of Tsunami’s library to maven on repo central. If
you are depending on these artifacts, you will soon have to migrate away. We are
planning to change the way we are publishing Tsunami’s dependencies. The plan is
not finalized yet, but most likely we will publish Tsunami directly on GitHub.
