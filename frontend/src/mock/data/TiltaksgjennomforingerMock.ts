import faker from 'faker';
import { Tiltaksgjennomforing } from '../../core/domain/Tiltaksgjennomforing';

const tiltaksgjennomforingerMock: Tiltaksgjennomforing[] = [
  {
    id: 1,
    tiltaksvariantId: 1,
    tiltaksnummer: faker.random.alphaNumeric(12),
    tittel: `Kjøreopplæring av ${faker.vehicle.manufacturer()}`,
    beskrivelse: faker.lorem.paragraph(1),
    fraDato: faker.date.past(3),
    tilDato: faker.date.future(3),
  },
  {
    id: 2,
    tiltaksvariantId: 1,
    tiltaksnummer: faker.random.alphaNumeric(12),
    tittel: `Kjøreopplæring av ${faker.vehicle.manufacturer()}`,
    beskrivelse: faker.lorem.paragraph(1),
    fraDato: faker.date.past(3),
    tilDato: faker.date.future(3),
  },
  {
    id: 3,
    tiltaksvariantId: 1,
    tiltaksnummer: faker.random.alphaNumeric(12),
    tittel: `Kjøreopplæring av ${faker.vehicle.manufacturer()}`,
    beskrivelse: faker.lorem.paragraph(1),
    fraDato: faker.date.past(3),
    tilDato: faker.date.future(3),
  },
  {
    id: 4,
    tiltaksvariantId: 1,
    tiltaksnummer: faker.random.alphaNumeric(12),
    tittel: `Kjøreopplæring av ${faker.vehicle.manufacturer()}`,
    beskrivelse: faker.lorem.paragraph(1),
    fraDato: faker.date.past(3),
    tilDato: faker.date.future(3),
  },
];

export { tiltaksgjennomforingerMock };
