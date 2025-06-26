# Core engine
FROM ghcr.io/google/tsunami-scanner-core:latest AS core

# Callback server
FROM ghcr.io/google/tsunami-security-scanner-callback-server:latest AS tcs

# Plugins
FROM ghcr.io/google/tsunami-plugins-google:latest AS plugins-google
FROM ghcr.io/google/tsunami-plugins-templated:latest AS plugins-templated
FROM ghcr.io/google/tsunami-plugins-doyensec:latest AS plugins-doyensec
FROM ghcr.io/google/tsunami-plugins-community:latest AS plugins-community
FROM ghcr.io/google/tsunami-plugins-govtech:latest AS plugins-govtech
FROM ghcr.io/google/tsunami-plugins-facebook:latest AS plugins-facebook

# Release a full version
FROM ubuntu:latest AS release

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates openjdk-21-jre golang \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /usr/share/doc && rm -rf /usr/share/man \
    && apt-get clean \
    && mkdir logs/

COPY --from=core /usr/tsunami/* /usr/tsunami/
COPY --from=tcs /usr/tsunami/* /usr/tsunami/

COPY --from=plugins-google /usr/tsunami/plugins/ /usr/tsunami/plugins/
COPY --from=plugins-templated /usr/tsunami/plugins/ /usr/tsunami/plugins/
COPY --from=plugins-doyensec /usr/tsunami/plugins/ /usr/tsunami/plugins/
COPY --from=plugins-community /usr/tsunami/plugins/ /usr/tsunami/plugins/
COPY --from=plugins-govtech /usr/tsunami/plugins/ /usr/tsunami/plugins/
COPY --from=plugins-facebook /usr/tsunami/plugins/ /usr/tsunami/plugins/

# Create wrapper scripts
WORKDIR /usr/tsunami
RUN echo '#!/bin/bash\njava -cp /usr/tsunami/tsunami.jar:/usr/tsunami/plugins/* -Dtsunami.config.location=/usr/tsunami/tsunami.yaml com.google.tsunami.main.cli.TsunamiCli $*\n' > /usr/bin/tsunami \
    && chmod +x /usr/bin/tsunami \
    && echo '#!/bin/bash\njava -cp /usr/tsunami/tsunami-tcs.jar com.google.tsunami.callbackserver.main.TcsMain --custom-config=/usr/tsunami/tcs_config.yaml $*\n' > /usr/bin/tsunami-tcs \
    && chmod +x /usr/bin/tsunami-tcs

# Install the linter
RUN go install github.com/google/tsunami-security-scanner-plugins/templated/utils/linter@latest \
    && ln -s /root/go/bin/linter /usr/bin/tsunami-linter
