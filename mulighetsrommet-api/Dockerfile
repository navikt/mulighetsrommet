FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:343fee3b95d33af866ca83fd226be0afc2a8235e7399b12a94ca2fc6ad075655
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

WORKDIR /app

COPY server/build/install/*/lib /lib

USER nonroot

ENV TZ="Europe/Oslo"
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.mulighetsrommet.api.ApplicationKt"]
