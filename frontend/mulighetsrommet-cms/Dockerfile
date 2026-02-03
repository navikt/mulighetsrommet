FROM cgr.dev/chainguard/nginx:latest AS runtime

LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"
LABEL org.opencontainers.image.title="mulighetsrommet-cms"

COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080
ENTRYPOINT ["/usr/sbin/nginx", "-g", "daemon off;"]
