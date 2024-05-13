FROM gcr.io/distroless/java21-debian12:nonroot
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"
COPY /build/libs/mulighetsrommet-api-all.jar app.jar
ENV TZ="Europe/Oslo"
EXPOSE 8080
CMD ["-jar", "app.jar"]
