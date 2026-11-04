FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/node:24-slim@sha256:c3f49b95f019101cf7165fdae4442b262594fb6c4b7ba109e03e28bc7fedf47d AS base

ENV NODE_ENV=production
ENV PORT="3000"

COPY ./dist /app

WORKDIR /app

EXPOSE 3000

ENV NODE_ENV=production
ENV VITE_MULIGHETSROMMET_API_BASE=http://mulighetsrommet-api
CMD ["./server/main.js"]
