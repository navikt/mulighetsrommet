import SanityClient from '@sanity/client';
import { rest, RestHandler } from 'msw';
import { badReq, ok } from './responses';
import { historikk } from '../fixtures/historikk';
import { DelMedBruker } from '../../../../mulighetsrommet-api-client/build/models/DelMedBruker';

export const apiHandlers: RestHandler[] = [
  rest.get('*/api/v1/bruker', req => {
    const fnr = req.url.searchParams.get('fnr');

    if (typeof fnr !== 'string') {
      return badReq("'fnr' must be specified");
    }

    return ok({
      fnr,
      innsatsgruppe: 'SITUASJONSBESTEMT_INNSATS',
      oppfolgingsenhet: {
        navn: 'NAV Lerkendal',
        enhetId: '5702',
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

  rest.get('*/api/v1/ansatt/me', () => {
    return ok({
      etternavn: 'VEILEDERSEN',
      fornavn: 'VEILEDER',
      ident: 'V12345',
      navn: 'Veiledersen, Veileder',
      tilganger: [],
    });
  }),

  rest.post('*/api/v1/dialog', () => {
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

    const result = await client.fetch(query, { enhetsId: 'enhet.lokal.5702', fylkeId: 'enhet.fylke.5700' });
    return ok(result);
  }),

  rest.get('*/api/v1/bruker/historikk', () => {
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
