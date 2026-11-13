FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:eddc3f7fdaa3fdc75ce43c988c9aec57f8e073d4e2f9e2116f149bb3bdac4205
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1500m"

WORKDIR /app
COPY build/install/*/lib /lib

USER nonroot

ENV TZ="Europe/Oslo"
EXPOSE 8080
CMD ["-jar", "app.jar"]
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.tiltak.okonomi.ApplicationKt"]
