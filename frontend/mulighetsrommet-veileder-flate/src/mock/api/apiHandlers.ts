import SanityClient from '@sanity/client';
import { rest, RestHandler } from 'msw';
import { badReq, ok } from './responses';
import { historikk } from '../fixtures/historikk';
import { DelMedBruker } from '../../../../mulighetsrommet-api-client/build/models/DelMedBruker';

export const apiHandlers: RestHandler[] = [
  rest.get('*/api/v1/bruker', (req, res, ctx) => {
    const fnr = req.url.searchParams.get('fnr');

    if (typeof fnr !== 'string') {
      return badReq("'fnr' must be specified");
    }

    return ok({
      fnr,
      innsatsgruppe: 'SITUASJONSBESTEMT_INNSATS',
      oppfolgingsenhet: {
        navn: 'NAV Fredrikstad',
        enhetId: '0106',
      },
      fornavn: 'IHERDIG',
      manuellStatus: {
        erUnderManuellOppfolging: false,
        krrStatus: {
          kanVarsles: true,
          erReservert: false,
        },
      },
    });
  }),

  rest.get('*/api/v1/veileder', (req, res, ctx) => {
    return ok({
      etternavn: 'VEILEDERSEN',
      fornavn: 'VEILEDER',
      ident: 'V12345',
      navn: 'Veiledersen, Veileder',
    });
  }),

  rest.post('*/api/v1/dialog', (req, res, ctx) => {
    return ok({
      id: '12345',
    });
  }),

  rest.get('*/api/v1/sanity', async req => {
    const query = req.url.searchParams.get('query');

    if (!(typeof query === 'string')) {
      return badReq("'query' must be specified");
    }

    const client = getSanityClient();

    const result = await client.fetch(query, { enhetsId: 'enhet.lokal.0106', fylkeId: 'enhet.fylke.5700' });
    return ok(result);
  }),

  rest.get('*/api/v1/bruker/historikk', async req => {
    const fnr = req.url.searchParams.get('fnr');

    if (!(typeof fnr === 'string')) {
      return badReq("'fnr' must be specified");
    }

    return ok(historikk);
  }),

  rest.post('*/api/v1/delMedBruker', async req => {
    const data = await req.json();
    return ok(data);
  }),

  rest.get('*/api/v1/delMedBruker/*', () => {
    return ok<DelMedBruker>({
      tiltaksnummer: '29518',
      navident: 'V15555',
      dialogId: '12345',
      bruker_fnr: '11223344557',
      created_at: new Date(2022, 2, 22).toString(),
    });
  }),

  rest.get('*/api/v1/delMedBruker', () => {
    return ok<DelMedBruker[]>([
      {
        "id": "5",
        "bruker_fnr": "12345678910",
        "navident": "V12345",
        "tiltaksnummer": "29518",
        "dialogId": "1234",
        "created_at": "2022-09-28T13:57:42.141479",
        "updated_at": "2022-09-28T13:57:42.141479",
        "created_by": "V12345",
        "updated_by": "V12345"
      },
      {
        "id": "3",
        "bruker_fnr": "12345678910",
        "navident": "V12345",
        "tiltaksnummer": "2974",
        "dialogId": "1234",
        "created_at": "2022-09-28T13:37:35.350350",
        "updated_at": "2022-09-28T13:37:35.350350",
        "created_by": "V12345",
        "updated_by": "V12345"
      }
    ]);
  }),
];

let cachedClient: ReturnType<typeof SanityClient> | null = null;

function getSanityClient() {
  if (cachedClient) {
    return cachedClient;
  }

  cachedClient = new SanityClient({
    apiVersion: '2022-06-20',
    projectId: import.meta.env.VITE_SANITY_PROJECT_ID,
    dataset: import.meta.env.VITE_SANITY_DATASET,
    token: import.meta.env.VITE_SANITY_ACCESS_TOKEN,
  });

  return cachedClient;
}
