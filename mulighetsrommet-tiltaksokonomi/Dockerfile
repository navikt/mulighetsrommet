FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:733de324e5d8742700e7f191f68d7ad7dd9d6477336ad8ef065c141cc586cc69
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1500m"

WORKDIR /app
COPY build/install/*/lib /lib

USER nonroot

ENV TZ="Europe/Oslo"
EXPOSE 8080
CMD ["-jar", "app.jar"]
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.tiltak.okonomi.ApplicationKt"]
