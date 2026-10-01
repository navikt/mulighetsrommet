FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/node:24-slim@sha256:7b17bfd96164028afa7631229f62b82eb3f0d3966fbf20dbd6980277cdd0aae4 AS base

ENV NODE_ENV=production
ENV PORT="3000"

COPY ./dist /app

WORKDIR /app

EXPOSE 3000

ENV NODE_ENV=production
ENV VITE_MULIGHETSROMMET_API_BASE=http://mulighetsrommet-api
CMD ["./server/main.js"]
