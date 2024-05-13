FROM ghcr.io/navikt/poao-baseimages/java:21
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1500m"

COPY /build/libs/mulighetsrommet-arena-adapter-all.jar app.jar
