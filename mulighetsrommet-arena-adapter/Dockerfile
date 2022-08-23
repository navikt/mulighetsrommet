FROM navikt/java:11
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"

ENV JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx2048m"

COPY /build/libs/mulighetsrommet-arena-adapter-all.jar app.jar
