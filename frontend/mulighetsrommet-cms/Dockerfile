FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/nginx:latest@sha256:fdd65ea225da60875e24a8b45d1e65501f9e4991b3b097ae65ed8a4c8f3782a3 AS runtime

LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"
LABEL org.opencontainers.image.title="mulighetsrommet-cms"

COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080
ENTRYPOINT ["/usr/sbin/nginx", "-g", "daemon off;"]
