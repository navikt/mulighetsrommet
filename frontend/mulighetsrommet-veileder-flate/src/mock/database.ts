import { nullable, oneOf, primaryKey } from '@mswjs/data';
import faker from 'faker';
import { innsatsgrupper } from './fixtures/innsatsgrupper';
import { tiltakstyper } from './fixtures/tiltakstyper';
import { createMockDatabase, idAutoIncrement } from './helpers';

export const definition = {
  innsatsgruppe: {
    id: primaryKey(Number),
    tittel: String,
    beskrivelse: String,
  },
  tiltakstype: {
    id: idAutoIncrement(),
    innsatsgruppe: oneOf('innsatsgruppe', { nullable: true }),
    sanityId: nullable(Number),
    tiltakskode: String,
    navn: String,
    fraDato: nullable(String),
    tilDato: nullable(String),
    createdBy: nullable(String),
    createdAt: nullable(String),
    updatedBy: nullable(String),
    updatedAt: nullable(String),
  },
  tiltaksgjennomforing: {
    id: idAutoIncrement(),
    tiltakskode: String,
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

  tiltakstyper.forEach(({ innsatsgruppe, ...data }) => {
    const relatedInnsatsgruppe = innsatsgruppe
      ? db.innsatsgruppe.findFirst({ where: { id: { equals: innsatsgruppe } } })
      : null;

    db.tiltakstype.create({
      innsatsgruppe: relatedInnsatsgruppe ?? undefined,
      ...data,
    });
  });

  db.tiltakstype.getAll().forEach((tiltakstype: any) => {
    for (let index = 0; index < faker.datatype.number({ min: 1, max: 5 }); index++) {
      db.tiltaksgjennomforing.create({
        tiltakskode: tiltakstype.tiltakskode,
        tiltaksnummer: faker.random.alphaNumeric(12),
        tittel: `Kjøreopplæring av ${faker.vehicle.manufacturer()}`,
        beskrivelse: faker.lorem.paragraph(1),
        tilDato: faker.date.future(3).toLocaleDateString(),
        fraDato: faker.date.past(3).toLocaleDateString(),
      });
    }
  });
});
