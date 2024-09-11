FROM node:20-bullseye-slim AS base

ENV NODE_ENV=production
ENV PORT="3000"

COPY ./dist /app

WORKDIR /app

EXPOSE 3000
CMD ["npm", "run", "start"]
