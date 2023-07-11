import { rest } from 'msw';
import { HistorikkForBruker } from 'mulighetsrommet-api-client';
import { historikk } from '../../fixtures/historikk';

export const historikkHandlers = [
  rest.get<any, any, HistorikkForBruker[]>('*/api/v1/internal/bruker/historikk', (_, res, ctx) => {
    return res(ctx.status(200), ctx.json(historikk));
  }),
];
