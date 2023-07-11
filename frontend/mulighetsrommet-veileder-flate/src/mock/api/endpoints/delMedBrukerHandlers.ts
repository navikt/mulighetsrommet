import { rest } from 'msw';
import { DelMedBruker, DialogResponse } from 'mulighetsrommet-api-client';

export const delMedBrukerHandlers = [
  rest.post<DelMedBruker, any, DelMedBruker>('*/api/v1/internal/delMedBruker', async (req, res, ctx) => {
    const data = (await req.json()) as DelMedBruker;
    return res(ctx.status(200), ctx.json(data));
  }),

  rest.get<any, any, DelMedBruker>('*/api/v1/internal/delMedBruker/*', (_, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({
        tiltaksnummer: '29518',
        navident: 'V15555',
        dialogId: '12345',
        bruker_fnr: '11223344557',
        createdAt: new Date(2022, 2, 22).toString(),
      })
    );
  }),

  rest.post<any, any, DialogResponse>('*/api/v1/internal/dialog', (_, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({
        id: '12345',
      })
    );
  }),
];
