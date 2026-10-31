FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:f3e4694f62a8d085a42d7347a6ff96e776363d7f5cb49fa36f4d1cfb90b3a5f1
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

WORKDIR /app

COPY server/build/install/*/lib /lib

USER nonroot

ENV XDG_CACHE_HOME="/tmp/.mulighetsrommet_api_cache"
ENV TZ="Europe/Oslo"
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.mulighetsrommet.api.ApplicationKt"]
