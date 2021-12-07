FROM nginx:stable-alpine
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"
LABEL org.opencontainers.image.title="mulighetsrommet-labs"
COPY build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
