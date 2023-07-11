import { rest } from 'msw';
import { Bruker, Innsatsgruppe } from 'mulighetsrommet-api-client';
import { ENHET_FREDRIKSTAD } from '../../mock_constants';
import { badReq } from '../responses';

export const brukerHandlers = [
  rest.get<any, any, Bruker>('*/api/v1/internal/bruker', (req, res, ctx) => {
    const fnr = req.url.searchParams.get('fnr');

    if (!fnr) {
      return badReq("'fnr' must be specified");
    }

    return res(
      ctx.status(200),
      ctx.json({
        fnr,
        //En bruker har enten servicegruppe eller innsatsgruppe. Denne kan endres ved behov
        innsatsgruppe: Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        // servicegruppe: 'BATT',
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
