FROM nginx:stable-alpine
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"
LABEL org.opencontainers.image.title="mulighetsrommet-dev"
COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
