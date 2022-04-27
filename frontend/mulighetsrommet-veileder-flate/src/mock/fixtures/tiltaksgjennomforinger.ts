import faker from 'faker';
import { Tiltaksgjennomforing, Tiltakskode, Tiltakstype } from '../../../../mulighetsrommet-api-client';
export const tiltaksgjennomforinger: Tiltaksgjennomforing[] = [
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    tittel: 'Opplæring',
    beskrivelse: faker.lorem.paragraph(3),
    tiltakskode: Tiltakskode.ABIST,
    tiltaksnummer: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    tittel: 'Opplæring',
    beskrivelse: faker.lorem.paragraph(3),
    tiltakskode: Tiltakskode.FUNKSJASS,
    tiltaksnummer: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    tittel: 'Opplæring',
    beskrivelse: faker.lorem.paragraph(3),
    tiltakskode: Tiltakskode.UTVOPPFOPL,
    tiltaksnummer: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    tittel: 'Opplæring',
    beskrivelse: faker.lorem.paragraph(3),
    tiltakskode: Tiltakskode.AVKLARAG,
    tiltaksnummer: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    tittel: 'Opplæring',
    beskrivelse: faker.lorem.paragraph(3),
    tiltakskode: Tiltakskode.GRUPPEAMO,
    tiltaksnummer: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
  },
];
