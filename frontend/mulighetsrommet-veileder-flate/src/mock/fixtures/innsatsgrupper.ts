import { Innsatsgruppe } from '../../core/api/models';
import { faker } from '@faker-js/faker';

export const innsatsgrupper: Innsatsgruppe[] = [
  {
    _id: '1',
    tittel: 'Standard innsats',
    beskrivelse: faker.lorem.paragraph(),
    nokkel: 'STANDARD_INNSATS',
  },
  {
    _id: '2',
    tittel: 'Situasjonsbestemt innsats',
    beskrivelse: faker.lorem.paragraph(),
    nokkel: 'SITUASJONSBESTEMT_INNSATS',
  },
  {
    _id: '3',
    tittel: 'Spesielt tilpasset innsats',
    beskrivelse: faker.lorem.paragraph(),
    nokkel: 'SPESIELT_TILPASSET_INNSATS',
  },
  {
    _id: '4',
    tittel: 'Varig tilpasset innsats',
    beskrivelse: faker.lorem.paragraph(),
    nokkel: 'VARIG_TILPASSET_INNSATS',
  },
];
