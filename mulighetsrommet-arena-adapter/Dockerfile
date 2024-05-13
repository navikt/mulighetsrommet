FROM gcr.io/distroless/java21-debian12:nonrootjava:21
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1500m"

COPY /build/libs/mulighetsrommet-arena-adapter-all.jar app.jar
