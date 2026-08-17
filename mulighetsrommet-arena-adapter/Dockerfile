FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:37186a87985f4477c0c8be30876db276e9233be4230672f8f5c526b2dcd0e39a
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1500m"

WORKDIR /app
COPY build/install/*/lib /lib

USER nonroot

ENV TZ="Europe/Oslo"
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.mulighetsrommet.arena.adapter.ApplicationKt"]
