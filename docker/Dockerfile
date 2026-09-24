FROM debian:bookworm-slim AS prepare

RUN apt-get update && apt-get -y install wget

# BellSoft names the ARM64 build "aarch64" while dpkg calls it "arm64".
RUN JDK_ARCH="$(dpkg --print-architecture)" \
 && if [ "$JDK_ARCH" = "arm64" ]; then JDK_ARCH=aarch64 ; fi \
 && mkdir /java && mkdir /ant \
 && { wget -q -O - "https://download.bell-sw.com/java/11.0.26+9/bellsoft-jdk11.0.26+9-linux-${JDK_ARCH}-full.tar.gz" | tar -xzf - -C /java --strip-component=1 ; } \
 && { wget -q -O - https://dlcdn.apache.org//ant/binaries/apache-ant-1.10.18-bin.tar.gz | tar -xzf - -C /ant --strip-component=1 ; }

FROM prepare AS build

ENV JAVA_HOME=/java
ENV ANT_HOME=/ant
ENV PATH="${PATH}:${JAVA_HOME}/bin:${ANT_HOME}/bin"

WORKDIR /workdir

CMD ["/bin/true"]
