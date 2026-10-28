FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:1323f5b7a9d94fa68b4545e870c20d8ed2e7a9cb8285a1dbbd6923074132b71b
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1500m"

WORKDIR /app
COPY build/install/*/lib /lib

USER nonroot

ENV TZ="Europe/Oslo"
EXPOSE 8080
CMD ["-jar", "app.jar"]
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.tiltak.okonomi.ApplicationKt"]
