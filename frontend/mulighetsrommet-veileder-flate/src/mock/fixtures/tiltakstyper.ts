import { faker } from '@faker-js/faker';
import { Tiltakstype } from '../../api/models';
export const tiltakstyper: Tiltakstype[] = [
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
      tittel: 'Varig tilpasset innsats',
      beskrivelse: faker.lorem.paragraph(),
      nokkel: 'VARIG_TILPASSET_INNSATS',
    },
  },

  // {
  //   _id: faker.datatype.number({ min: 100000, max: 999999 }),
  //   innsatsgruppe: '1',
  //   tiltakstypeNavn: 'Funksjonsassistanse',
  // },
  // {
  //   _id: faker.datatype.number({ min: 100000, max: 999999 }),
  //   innsatsgruppe: '1',
  //   tiltakstypeNavn: 'Utvidet oppfølging',
  // },
  // {
  //   _id: faker.datatype.number({ min: 100000, max: 999999 }),
  //   innsatsgruppe: '1',
  //   tiltakstypeNavn: 'Avklaring',
  // },
];
