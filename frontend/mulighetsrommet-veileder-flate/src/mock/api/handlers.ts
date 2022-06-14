import { rest, RestHandler } from 'msw';
import { db } from '../database';
import { toTiltaksgjennomforing } from '../entities/tiltaksgjennomforing';
import { toTiltakstype } from '../entities/tiltakstype';
import { mockFeatures } from './features';
import { badReq, notFound, ok } from './responses';
import SanityClient from '@sanity/client';

const client = new SanityClient({
  apiVersion: "2020-06-01",
  projectId: import.meta.env.VITE_SANITY_PROJECT_ID,
  dataset: import.meta.env.VITE_SANITY_DATASET,
  token: import.meta.env.VITE_SANITY_ACCESS_TOKEN,
});

export const handlers: RestHandler[] = [
  rest.get('*/api/feature', (req, res, ctx) => {
    return res(ctx.delay(500), ctx.json(mockFeatures));
  }),

  rest.get('*/api/v1/innsatsgrupper', () => {
    return ok(db.innsatsgruppe.getAll());
  }),

  rest.get('*/api/v1/tiltakstyper', () => {
    return ok(db.tiltakstype.getAll().map(toTiltakstype));
  }),

  rest.get('*/api/v1/tiltaksgjennomforinger', () => {
    return ok(db.tiltaksgjennomforing.getAll().map(toTiltaksgjennomforing));
  }),
  rest.get('*/api/v1/tiltaksgjennomforinger/:id', req => {
    const { id } = req.params;

    const entity = db.tiltaksgjennomforing.findFirst({
      where: { id: { equals: Number(id) } },
    });

    if (!entity) {
      return notFound();
    }

    return ok(toTiltaksgjennomforing(entity));
  }),

  rest.get('*/api/v1/sanity', async req => {
    const query = req.url.searchParams.get('query');

    if (!(typeof query === 'string')) {
      return badReq("'query' must be specified");
    }

    const result = await client.fetch(query);

    return ok({
      ms: 100,
      query,
      result,
    });
  }),
];
