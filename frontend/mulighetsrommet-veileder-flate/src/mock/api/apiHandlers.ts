import SanityClient from '@sanity/client';
import { rest, RestHandler } from 'msw';
import { badReq, ok } from './responses';
import { mockFeatures } from './features';

export const apiHandlers: RestHandler[] = [
  rest.get('*/api/v1/bruker/:fnr', (req, res, ctx) => {
    const { fnr } = req.params;
    return ok({
      fnr,
      innsatsgruppe: 'SITUASJONSBESTEMT_INNSATS',
      oppfolgingsenhet: {
        navn: 'NAV Fredrikstad',
        enhetId: '0106',
      },
      fornavn: 'Iherdig',
    });
  }),

  rest.get('*/api/v1/sanity', async req => {
    const query = req.url.searchParams.get('query');

    if (!(typeof query === 'string')) {
      return badReq("'query' must be specified");
    }

    const client = getSanityClient();

    const result = await client.fetch(query, { enhetsId: '*', fylkeId: '5700' });
    return ok(result);
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
