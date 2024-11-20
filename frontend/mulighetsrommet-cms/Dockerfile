FROM nginxinc/nginx-unprivileged AS builder

COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

FROM gcr.io/distroless/base-debian12:nonroot

LABEL org.opencontainers.image.source="https://github.com/navikt/mulighetsrommet"
LABEL org.opencontainers.image.title="mulighetsrommet-cms"

COPY --from=builder /lib/x86_64-linux-gnu/libcrypt.so.1 /lib/x86_64-linux-gnu/libcrypt.so.1
COPY --from=builder /lib/x86_64-linux-gnu/libpcre2-8.so.0 /lib/x86_64-linux-gnu/libpcre2-8.so.0
COPY --from=builder /lib/x86_64-linux-gnu/libz.so.1 /lib/x86_64-linux-gnu/libz.so.1

COPY --from=builder /etc/nginx /etc/nginx
COPY --from=builder /var/log/nginx /var/log/nginx
COPY --from=builder /usr/sbin/nginx /usr/bin/nginx
COPY --from=builder /usr/share/nginx/html /usr/share/nginx/html
COPY --from=builder /etc/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080
ENTRYPOINT ["/usr/bin/nginx", "-g", "daemon off;"]
