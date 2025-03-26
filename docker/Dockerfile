FROM debian:bookworm-slim AS prepare

RUN apt-get update && apt-get -y install wget

RUN mkdir /java && mkdir /ant \
 && { wget -q -O - https://download.bell-sw.com/java/11.0.26+9/bellsoft-jdk11.0.26+9-linux-amd64-full.tar.gz | tar -xzf - -C /java --strip-component=1 ; } \
 && { wget -q -O - https://dlcdn.apache.org//ant/binaries/apache-ant-1.10.15-bin.tar.gz | tar -xzf - -C /ant --strip-component=1 ; }

FROM prepare AS build

ENV JAVA_HOME=/java
ENV ANT_HOME=/ant
ENV PATH="${PATH}:${JAVA_HOME}/bin:${ANT_HOME}/bin"

WORKDIR /workdir

CMD ["/bin/true"]
