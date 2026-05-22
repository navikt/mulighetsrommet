FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/node:24-slim@sha256:236e984f0f5cc6fe1af9b3d249ce8b0a4ca780f95529b7e9a9d94d23c1393528 AS base

ENV NODE_ENV=production
ENV PORT="3000"

COPY ./dist /app

WORKDIR /app

EXPOSE 3000

ENV NODE_ENV=production
ENV VITE_MULIGHETSROMMET_API_BASE=http://mulighetsrommet-api
CMD ["./server/main.js"]
