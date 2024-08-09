FROM gcr.io/distroless/java21-debian12:nonroot
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1500m"

WORKDIR /app
COPY /build/libs/mulighetsrommet-tiltakshistorikk-all.jar app.jar

USER nonroot

ENV TZ="Europe/Oslo"
EXPOSE 8080
CMD ["-jar", "app.jar"]
