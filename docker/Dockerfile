FROM ubuntu:24.04
ARG IDEASY_VERSION=2026.08.001
ENV IDE_ROOT=/projects
RUN apt-get update \
&& apt-get install -y --no-install-recommends \
ca-certificates \
curl \
git \
gzip \
tar \
bash \
libfreetype6 \
fontconfig \
libx11-6 \
libxext6 \
libxrender1 \
libxtst6 \
libxi6 \
libxrandr2 \
libxinerama1 \
libxcursor1 \
libxcomposite1 \
libxdamage1 \
openssh-client \
libgtk-3-0 \
libglib2.0-0 \
libatk1.0-0 \
libatk-bridge2.0-0 \
libcairo2 \
libpango-1.0-0 \
libgdk-pixbuf-2.0-0 \
libnss3 \
libgbm1 \
libasound2t64 \
&& rm -rf /var/lib/apt/lists/*

# GitHub Copilot CLI

RUN apt-get update \
&& apt-get install -y nodejs npm \
&& npm install -g @github/copilot \
&& rm -rf /var/lib/apt/lists/*

# IDEasy

RUN mkdir -p /opt/ideasy "${IDE_ROOT}" \
&& curl -fsSL \
"https://repo1.maven.org/maven2/com/devonfw/tools/IDEasy/ide-cli/${IDEASY_VERSION}/ide-cli-${IDEASY_VERSION}-linux-x64.tar.gz" \
-o /tmp/ideasy.tar.gz \
&& tar --extract --gzip --no-same-owner \
--file /tmp/ideasy.tar.gz \
--directory /opt/ideasy \
&& cd /opt/ideasy \
&& ./bin/ideasy --batch --force install \
&& rm /tmp/ideasy.tar.gz

ENV PATH="${IDE_ROOT}/_ide/installation/bin:${PATH}"

RUN ideasy --batch create basic-project -

WORKDIR /projects/basic-project

CMD ["bash"]
