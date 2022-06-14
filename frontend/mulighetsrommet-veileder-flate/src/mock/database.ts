import { nullable, oneOf, primaryKey } from '@mswjs/data';
import faker from 'faker';
import { innsatsgrupperFixture } from './fixtures/innsatsgrupper';
import { tiltakstyper } from './fixtures/tiltakstyper';
import { createMockDatabase, idAutoIncrement } from './helpers';
import {Tiltakstype} from "../api/models";

export const definition = {
  innsatsgruppe: {
    id: primaryKey(Number),
    navn: String,
  },
  tiltakstype: {
    id: idAutoIncrement(),
    tiltakstypeNavn: String,
    beskrivelse: String,
    innsatsgruppe: String,
    varighet: String,
    regelverkFil: String, //skal være fil
    regelverkFilNavn: String,
    regelverkLenke: String,
    regelverkLenkeNavn: String,
    faneinnhold: {
      forHvemInfoboks: String,
      forHvem: Object,
      detaljerOgInnholdInfoboks: String,
      detaljerOgInnhold: Object,
      pameldingOgVarighetInfoboks: String,
      pameldingOgVarighet: Object,
    },
  },
  tiltaksgjennomforing: {
    id: idAutoIncrement(),
    tiltakstype: Object,
    tiltaksgjennomforingNavn: String,
    beskrivelse: String,
    tiltaksnummer: Number,
    kontaktinfoArrangor: {
      id: Number,
      selskapsnavn: String,
      telefonnummer: String,
      epost: String,
      adresse: String,
    },
    lokasjon: String,
    oppstart: String,
    oppstartsdato: Date,
    faneinnhold: {
      forHvemInfoboks: String,
      forHvem: Object,
      detaljerOgInnholdInfoboks: String,
      detaljerOgInnhold: Object,
      pameldingOgVarighetInfoboks: String,
      pameldingOgVarighet: Object,
    },
    kontaktinfoTiltaksansvarlig: {
      id: Number,
      navn: String,
      enhet: String,
      telefonnummer: String,
      epost: String,
    },
  },
};

export type DatabaseDictionary = typeof definition;

export const db = createMockDatabase(definition, (db: any) => {
  innsatsgrupperFixture.forEach(db.innsatsgruppe.create);

  tiltakstyper.forEach(({ innsatsgruppe, ...data }) => {
    const relatedInnsatsgruppe = innsatsgruppe
      ? db.innsatsgruppe.findFirst({ where: { id: { equals: innsatsgruppe } } })
      : null;

    db.tiltakstype.create({
      innsatsgruppe: relatedInnsatsgruppe ?? undefined,
      ...data,
    });
  });

  db.tiltakstype.getAll().forEach((tiltakstype: Tiltakstype) => {
    for (let index = 0; index < faker.datatype.number({ min: 1, max: 5 }); index++) {
      db.tiltaksgjennomforing.create({
        tiltaksnummer: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
        tittel: `Kjøreopplæring av ${faker.vehicle.manufacturer()}`,
        beskrivelse: faker.lorem.paragraph(1),
        tilDato: faker.date.future(2).toISOString(),
        fraDato: faker.date.future(2).toISOString(),
      });
    }
  });
});
