import { faker } from '@faker-js/faker';
import { Innsatsgruppe, SanityInnsatsgruppe, SanityTiltakstype } from 'mulighetsrommet-api-client';
export const tiltakstyper: SanityTiltakstype[] = [
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
    tiltakstypeNavn: 'VTA',
    beskrivelse: faker.lorem.paragraph(2),
    nokkelinfoKomponenter: [
      {
        _id: faker.datatype.number().toString(),
        tittel: 'Overgang til arbeid',
        innhold: '69%',
        hjelpetekst: 'Test',
      },
    ],
    innsatsgruppe: {
      _id: '4',
      tittel: SanityInnsatsgruppe.tittel.VARIG_TILPASSET_INNSATS,
      beskrivelse: faker.lorem.paragraph(),
      nokkel: Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    },
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
    tiltakstypeNavn: 'Oppfølging',
    beskrivelse: faker.lorem.paragraph(2),
    innsatsgruppe: {
      _id: '1',
      tittel: SanityInnsatsgruppe.tittel.STANDARD_INNSATS,
      beskrivelse: faker.lorem.paragraph(),
      nokkel: Innsatsgruppe.STANDARD_INNSATS,
    },
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
    tiltakstypeNavn: 'Avklaring',
    beskrivelse: faker.lorem.paragraph(2),
    innsatsgruppe: {
      _id: '2',
      tittel: SanityInnsatsgruppe.tittel.SITUASJONSBESTEMT_INNSATS,
      beskrivelse: faker.lorem.paragraph(),
      nokkel: Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
    },
  },
];
