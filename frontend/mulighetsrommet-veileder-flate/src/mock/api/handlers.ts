import SanityClient from '@sanity/client';
import { rest, RestHandler } from 'msw';
import { mockFeatures } from './features';
import { badReq, ok } from './responses';

export const handlers: RestHandler[] = [
  rest.get('*/api/feature', (req, res, ctx) => {
    return res(ctx.delay(500), ctx.json(mockFeatures));
  }),

  rest.get('*/api/v1/bruker/:fnr', (req, res, ctx) => {
    const { fnr } = req.params;
    return ok({
      fnr,
      innsatsgruppe: 'SITUASJONSBESTEMT_INNSATS',
      oppfolgingsenhet: 'NAV Fredrikstad',
    });
  }),

  rest.get('*/api/v1/sanity', async req => {
    const query = req.url.searchParams.get('query');

    if (!(typeof query === 'string')) {
      return badReq("'query' must be specified");
    }

    const client = getSanityClient();

    const result = await client.fetch(query);
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
