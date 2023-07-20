import { rest } from 'msw';
import { NavVeileder } from 'mulighetsrommet-api-client';

export const veilederHandlers = [
  rest.get<DefaultBodyType, PathParams, NavVeileder>('*/api/v1/internal/veileder/me', (_, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({
        navIdent: 'V12345',
        etternavn: 'VEILEDERSEN',
        fornavn: 'VEILEDER',
        hovedenhet: {
          enhetsnummer: '2990',
          navn: 'Østfold',
        },
      })
    );
  }),
];
