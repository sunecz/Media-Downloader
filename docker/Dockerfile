FROM docker.io/amd64/eclipse-temurin:11-alpine
COPY apache-ant-1.10.11-r0.tar /apache-ant.tar

RUN tar -xf /apache-ant.tar -C / \
 && rm -f /apache-ant.tar \
 && apk add bash p7zip

WORKDIR /workdir

CMD ["/bin/true"]
