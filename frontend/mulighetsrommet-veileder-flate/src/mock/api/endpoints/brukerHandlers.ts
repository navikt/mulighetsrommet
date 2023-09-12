import { DefaultBodyType, PathParams, rest } from 'msw';
import { Bruker, Innsatsgruppe } from 'mulighetsrommet-api-client';
import { ENHET_FREDRIKSTAD } from '../../mock_constants';
import { badReq } from '../responses';

export const brukerHandlers = [
  rest.get<DefaultBodyType, PathParams, Bruker>('*/api/v1/internal/bruker', (req, res, ctx) => {
    const fnr = req.url.searchParams.get('fnr');

    if (!fnr) {
      return badReq("'fnr' must be specified");
    }

    return res(
      ctx.status(200),
      ctx.json({
        fnr,
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
];
