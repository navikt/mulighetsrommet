FROM node:20-alpine AS base

ENV NODE_ENV production
ENV PORT="3000"

WORKDIR /app

COPY .npmrc package*.json /app/

RUN --mount=type=secret,id=NODE_AUTH_TOKEN \
    npm config set //npm.pkg.github.com/:_authToken=$(cat /run/secrets/NODE_AUTH_TOKEN)
RUN npm config set @navikt:registry=https://npm.pkg.github.com
RUN npm install --no-audit --ignore-scripts

COPY build/ /app/build/
COPY ./server.js /app/server.js

EXPOSE 3000
CMD ["npm", "run", "start"]
