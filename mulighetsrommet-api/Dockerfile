FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:a250e3b81efa13b1c5636e6bcaf4a4010209bf225c45462bd36ba8c43ec888bd
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

WORKDIR /app

COPY server/build/install/*/lib /lib

USER nonroot

ENV TZ="Europe/Oslo"
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.mulighetsrommet.api.ApplicationKt"]
