FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:3620c8af1446458d53ee505dc135f9f01a9b73ae3dad04299f0158df2737ee9b
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1500m"

WORKDIR /app
COPY build/install/*/lib /lib

USER nonroot

ENV TZ="Europe/Oslo"
EXPOSE 8080
CMD ["-jar", "app.jar"]
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.tiltak.okonomi.ApplicationKt"]
