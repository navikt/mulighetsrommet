import { DefaultBodyType, PathParams, rest } from 'msw';
import { Bruker, GetBrukerRequest, HistorikkForBruker, Innsatsgruppe } from 'mulighetsrommet-api-client';
import { ENHET_FREDRIKSTAD } from '../../mock_constants';
import { badReq } from '../responses';
import { historikk } from '../../fixtures/historikk';

export const brukerHandlers = [
  rest.post<DefaultBodyType, PathParams, Bruker>('*/api/v1/internal/bruker', async (req, res, ctx) => {
    const { norskIdent } = await req.json<GetBrukerRequest>();

    if (!norskIdent) {
      return badReq("'fnr' must be specified");
    }

    return res(
      ctx.status(200),
      ctx.json({
        fnr: norskIdent,
        innsatsgruppe: Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        oppfolgingsenhet: {
          navn: 'NAV Fredrikstad',
          enhetId: ENHET_FREDRIKSTAD,
        },
        fornavn: 'IHERDIG',
        geografiskEnhet: {
          navn: 'NAV Fredrikstad',
          enhetsnummer: ENHET_FREDRIKSTAD,
        },
        manuellStatus: {
          erUnderManuellOppfolging: false,
          krrStatus: {
            kanVarsles: true,
            erReservert: false,
          },
        },
      })
    );
  }),

  rest.post<DefaultBodyType, PathParams, HistorikkForBruker[]>('*/api/v1/internal/bruker/historikk', (_, res, ctx) => {
    return res(ctx.status(200), ctx.json(historikk));
  }),
];
