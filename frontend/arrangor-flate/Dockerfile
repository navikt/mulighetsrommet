FROM node:20-bullseye-slim AS base
# FROM gcr.io/distroless/nodejs20-debian11 as production

# ENV NODE_ENV production

# Setup production node_modules
FROM base AS production-deps

WORKDIR /app

ADD package.json /app/package.json

# RUN npm install --omit=dev
RUN npm install

FROM base

ENV PORT="3000"

WORKDIR /app

COPY --from=production-deps /app/node_modules/ /app/node_modules/
COPY build/ /app/build/
COPY server.js /app/server.js
COPY public/ /app/public/
COPY package.json /app/package.json

EXPOSE 3000

CMD ["npm", "run", "start"]
