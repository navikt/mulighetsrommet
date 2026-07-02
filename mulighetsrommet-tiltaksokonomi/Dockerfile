FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:d7df8d7afff31431b0ac68d6f82306c87d6e83145db874376064b847da61bd02
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1500m"

WORKDIR /app
COPY build/install/*/lib /lib

USER nonroot

ENV TZ="Europe/Oslo"
EXPOSE 8080
CMD ["-jar", "app.jar"]
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.tiltak.okonomi.ApplicationKt"]
