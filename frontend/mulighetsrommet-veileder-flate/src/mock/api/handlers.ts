import { rest, RestHandler } from 'msw';
import { db } from '../database';
import { toTiltaksgjennomforing } from '../entities/tiltaksgjennomforing';
import { toTiltakstype } from '../entities/tiltakstype';
import { mockFeatures } from './features';
import { notFound, ok } from './responses';

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

  rest.get('*/api/v1/tiltaksgjennomforinger', req => {
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
];
