import { oneOf, primaryKey } from '@mswjs/data';
import faker from 'faker';
import { innsatsgrupper } from './fixtures/innsatsgrupper';
import { tiltaksvarianter } from './fixtures/tiltaksvarianter';
import { createMockDatabase, idAutoIncrement } from './helpers';

export const definition = {
  innsatsgruppe: {
    id: primaryKey(Number),
    tittel: String,
    beskrivelse: String,
  },
  tiltaksvariant: {
    id: idAutoIncrement(),
    innsatsgruppe: oneOf('innsatsgruppe', { nullable: true }),
    tittel: String,
    beskrivelse: String,
    ingress: String,
  },
  tiltaksgjennomforing: {
    id: idAutoIncrement(),
    tiltaksvariantId: oneOf('tiltaksvariant'),
    tiltaksnummer: String,
    tittel: String,
    beskrivelse: String,
    fraDato: Date,
    tilDato: Date,
  },
};

export type DatabaseDictionary = typeof definition;

export const db = createMockDatabase(definition, (db: any) => {
  innsatsgrupper.forEach(db.innsatsgruppe.create);

  tiltaksvarianter.forEach(({ innsatsgruppe, ...data }) => {
    const relatedInnsatsgruppe = innsatsgruppe
      ? db.innsatsgruppe.findFirst({ where: { id: { equals: innsatsgruppe } } })
      : null;

    db.tiltaksvariant.create({
      innsatsgruppe: relatedInnsatsgruppe ?? undefined,
      ...data,
    });
  });

  db.tiltaksvariant.getAll().forEach((tiltaksvariant: any) => {
    for (let index = 0; index < faker.datatype.number({ min: 1, max: 5 }); index++) {
      db.tiltaksgjennomforing.create({
        tiltaksvariantId: tiltaksvariant,
        tiltaksnummer: faker.random.alphaNumeric(12),
        tittel: `Kjøreopplæring av ${faker.vehicle.manufacturer()}`,
        beskrivelse: faker.lorem.paragraph(1),
        tilDato: faker.date.future(3).toLocaleDateString(),
        fraDato: faker.date.past(3).toLocaleDateString(),
      });
    }
  });
});
