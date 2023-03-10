import { faker } from '@faker-js/faker';
import { Innsatsgruppe, SanityInnsatsgruppe } from 'mulighetsrommet-api-client';

export const innsatsgrupper: SanityInnsatsgruppe[] = [
  {
    _id: '1',
    tittel: SanityInnsatsgruppe.tittel.STANDARD_INNSATS,
    beskrivelse: faker.lorem.paragraph(),
    nokkel: Innsatsgruppe.STANDARD_INNSATS,
  },
  {
    _id: '2',
    tittel: SanityInnsatsgruppe.tittel.SITUASJONSBESTEMT_INNSATS,
    beskrivelse: faker.lorem.paragraph(),
    nokkel: Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
  },
  {
    _id: '3',
    tittel: SanityInnsatsgruppe.tittel.SPESIELT_TILPASSET_INNSATS,
    beskrivelse: faker.lorem.paragraph(),
    nokkel: Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
  },
  {
    _id: '4',
    tittel: SanityInnsatsgruppe.tittel.VARIG_TILPASSET_INNSATS,
    beskrivelse: faker.lorem.paragraph(),
    nokkel: Innsatsgruppe.VARIG_TILPASSET_INNSATS,
  },
];
