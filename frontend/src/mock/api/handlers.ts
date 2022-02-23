import { rest, RestHandler, RestRequest } from 'msw';
import { Tiltakstype } from '../../api';
import { db } from '../database';
import { toTiltaksgjennomforing } from '../entities/tiltaksgjennomføring';
import { toTiltakstype } from '../entities/tiltakstype';
import { notFound, ok } from './responses';
import { mockFeatures } from './data';

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
  rest.get('*/api/tiltakstyper/:id', req => {
    const { id } = req.params;

    const entity = db.tiltakstype.findFirst({
      where: { id: { equals: Number(id) } },
    });

    if (!entity) {
      return notFound();
    }

    return ok(toTiltakstype(entity));
  }),
  rest.post('*/api/tiltakstyper', (req: RestRequest<Tiltakstype>) => {
    const innsatsgruppe = db.innsatsgruppe.findFirst({
      where: { id: { equals: Number(req.body.innsatsgruppe) } },
    });

    const entity = db.tiltakstype.create({ ...req.body, innsatsgruppe: innsatsgruppe ?? undefined });

    return ok(entity);
  }),
  rest.put('*/api/tiltakstyper/:id', (req: RestRequest<Tiltakstype>) => {
    const { id } = req.params;

    const innsatsgruppe = db.innsatsgruppe.findFirst({
      where: { id: { equals: Number(req.body.innsatsgruppe) } },
    });

    const entity = db.tiltakstype.update({
      where: { id: { equals: Number(id) } },
      data: { ...req.body, innsatsgruppe: innsatsgruppe ?? undefined },
    });

    if (!entity) {
      return notFound();
    }

    return ok(toTiltakstype(entity));
  }),
  rest.delete('*/api/tiltakstyper/:id', req => {
    const { id } = req.params;

    const entity = db.tiltakstype.delete({
      where: { id: { equals: Number(id) } },
    });

    return entity ? ok({}) : notFound();
  }),
  rest.get('*/api/tiltakstyper/:id/tiltaksgjennomforinger', req => {
    const { id } = req.params;

    const items = db.tiltaksgjennomforing.findMany({
      where: { tiltakstypeId: { id: { equals: Number(id) } } },
    });

    return ok(items.map(toTiltaksgjennomforing));
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
