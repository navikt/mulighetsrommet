FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:3ce7cf0bf71e5540a7ec9b63661fd2a86446a52080b31cf13752ea463b7c8483
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1500m"

WORKDIR /app

COPY build/install/*/lib /lib

USER nonroot

ENV TZ="Europe/Oslo"
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.tiltak.historikk.ApplicationKt"]
