FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:b6252714c5b59256b44f51fa5adcf0b1725d2daf425e3824236efbea1a0d97d4
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

WORKDIR /app

COPY server/build/install/*/lib /lib

USER nonroot

ENV XDG_CACHE_HOME="/tmp/.mulighetsrommet_api_cache"
ENV TZ="Europe/Oslo"
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.mulighetsrommet.api.ApplicationKt"]
