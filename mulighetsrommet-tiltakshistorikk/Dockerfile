FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:3bf34fd949124081777c356147320f13a02b1d935dc7ccd700cd83c0f0f43488
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1500m"

WORKDIR /app

COPY build/install/*/lib /lib

USER nonroot

ENV TZ="Europe/Oslo"
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.tiltak.historikk.ApplicationKt"]
