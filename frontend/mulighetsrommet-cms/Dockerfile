FROM nginxinc/nginx-unprivileged
LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"
LABEL org.opencontainers.image.title="mulighetsrommet-cms"
COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
