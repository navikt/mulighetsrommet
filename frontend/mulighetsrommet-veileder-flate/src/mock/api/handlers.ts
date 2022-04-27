import { rest, RestHandler } from 'msw';
import { db } from '../database';
import { toTiltaksgjennomforing } from '../entities/tiltaksgjennomføring';
import { toTiltakstype } from '../entities/tiltakstype';
import { badReq, notFound, ok } from './responses';
import { mockFeatures } from './features';

export const handlers: RestHandler[] = [
  rest.get('*/api/feature', (req, res, ctx) => {
    return res(ctx.delay(500), ctx.json(mockFeatures));
  }),

  rest.get('*/api/innsatsgrupper', () => {
    return ok(db.innsatsgruppe.getAll());
  }),

  rest.get('*/api/tiltakstyper', () => {
    return ok(db.tiltakstype.getAll().map(toTiltakstype));
  }),
  rest.get('*/api/tiltakstyper/:tiltakskode', req => {
    const { tiltakskode } = req.params as any;

    if (!tiltakskode) {
      return badReq();
    }

    const entity = db.tiltakstype.findFirst({
      where: { tiltakskode: { equals: tiltakskode } },
    });

    if (!entity) {
      return notFound();
    }

    return ok(toTiltakstype(entity));
  }),
  rest.get('*/api/tiltakstyper/:tiltakskode/tiltaksgjennomforinger', req => {
    const { tiltakskode } = req.params as any;

    const items = db.tiltaksgjennomforing.findMany({
      where: { tiltakskode: { equals: tiltakskode } },
    });

    return ok(items.map(toTiltaksgjennomforing));
  }),
  rest.get('*/api/tiltaksgjennomforinger', req => {
    return ok(db.tiltaksgjennomforing.getAll().map(toTiltaksgjennomforing));
  }),
  rest.get('*/api/tiltaksgjennomforinger/:id', req => {
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
