FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/node:24-slim@sha256:a66e5c877724ed04fe62a11152aab13627cbc69ad63c197a7ea44b47ec266924 AS base

ENV NODE_ENV=production
ENV PORT="3000"

COPY ./dist /app

WORKDIR /app

EXPOSE 3000

ENV NODE_ENV=production
ENV VITE_MULIGHETSROMMET_API_BASE=http://mulighetsrommet-api
CMD ["./server/main.js"]
