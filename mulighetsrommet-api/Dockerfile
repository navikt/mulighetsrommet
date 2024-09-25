FROM gcr.io/distroless/java21-debian12:nonroot
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

WORKDIR /app

COPY data data
COPY templates templates
COPY resources resources
COPY fonts fonts

COPY /build/libs/mulighetsrommet-api-all.jar app.jar

USER nonroot

ENV TZ="Europe/Oslo"
EXPOSE 8080
CMD ["-jar", "app.jar"]
