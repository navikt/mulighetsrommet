FROM ghcr.io/navikt/poao-baseimages/java:17
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1500m"

COPY /build/libs/mulighetsrommet-tiltakshistorikk-all.jar app.jar
