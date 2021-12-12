import { rest, RestHandler, RestRequest } from 'msw';
import { Tiltaksvariant } from '../../api';
import { db } from '../database';
import { toTiltaksgjennomforing } from '../entities/tiltaksgjennomføring';
import { toTiltaksvariant } from '../entities/tiltaksvariant';
import { notFound, ok } from './responses';

export const handlers: RestHandler[] = [
  rest.get('*/api/innsatsgrupper', () => {
    return ok(db.innsatsgruppe.getAll());
  }),

  rest.get('*/api/tiltaksvarianter', () => {
    return ok(db.tiltaksvariant.getAll().map(toTiltaksvariant));
  }),
  rest.get('*/api/tiltaksvarianter/:id', req => {
    const { id } = req.params;

    const entity = db.tiltaksvariant.findFirst({
      where: { id: { equals: Number(id) } },
    });

    if (!entity) {
      return notFound();
    }

    return ok(toTiltaksvariant(entity));
  }),
  rest.post('*/api/tiltaksvarianter', (req: RestRequest<Tiltaksvariant>) => {
    const innsatsgruppe = db.innsatsgruppe.findFirst({
      where: { id: { equals: Number(req.body.innsatsgruppe) } },
    });

    const entity = db.tiltaksvariant.create({ ...req.body, innsatsgruppe: innsatsgruppe ?? undefined });

    return ok(entity);
  }),
  rest.put('*/api/tiltaksvarianter/:id', (req: RestRequest<Tiltaksvariant>) => {
    const { id } = req.params;

    const innsatsgruppe = db.innsatsgruppe.findFirst({
      where: { id: { equals: Number(req.body.innsatsgruppe) } },
    });

    const entity = db.tiltaksvariant.update({
      where: { id: { equals: Number(id) } },
      data: { ...req.body, innsatsgruppe: innsatsgruppe ?? undefined },
    });

    if (!entity) {
      return notFound();
    }

    return ok(toTiltaksvariant(entity));
  }),
  rest.delete('*/api/tiltaksvarianter/:id', req => {
    const { id } = req.params;

    const entity = db.tiltaksvariant.delete({
      where: { id: { equals: Number(id) } },
    });

    return entity ? ok({}) : notFound();
  }),
  rest.get('*/api/tiltaksvarianter/:id/tiltaksgjennomforinger', req => {
    const { id } = req.params;

    const items = db.tiltaksgjennomforing.findMany({
      where: { tiltaksvariantId: { id: { equals: Number(id) } } },
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
