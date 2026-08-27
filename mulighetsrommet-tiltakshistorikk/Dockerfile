FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:7090031b4682ffa879328f78185aa2d480775a1e5c7c147e6946429a4e30b6fd
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1500m"

WORKDIR /app

COPY build/install/*/lib /lib

USER nonroot

ENV TZ="Europe/Oslo"
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.tiltak.historikk.ApplicationKt"]
