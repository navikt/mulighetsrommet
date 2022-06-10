import faker from 'faker';
import { Tiltakstype } from '../../../../mulighetsrommet-api-client';
export const tiltakstyper: Tiltakstype[] = [
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '1',
    tiltakstypeNavn: 'Opplæring',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '1',
    tiltakstypeNavn: 'Funksjonsassistanse',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '1',
    tiltakstypeNavn: 'Utvidet oppfølging',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '1',
    tiltakstypeNavn: 'Avklaring',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '1',
    tiltakstypeNavn: 'Arbeidsmarkedsopplæring (AMO)',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '1',
    tiltakstypeNavn: 'Ekspertbistand',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '1',
    tiltakstypeNavn: 'Jobbklubb',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '2',
    tiltakstypeNavn: 'Oppfølging',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '2',
    tiltakstypeNavn: 'Digital jobbklubb',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '2',
    tiltakstypeNavn: 'Fag- og yrkesopplæring',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '2',
    tiltakstypeNavn: 'Arbeidstrening',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '2',
    tiltakstypeNavn: 'Arbeidsforberedende trening',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '2',
    tiltakstypeNavn: 'Midlertidig lønnstilskudd',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '2',
    tiltakstypeNavn: 'Varig lønnstilskudd',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '3',
    tiltakstypeNavn: 'Varig tilrettelagt arbeid i skjermet virksomhet',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '3',
    tiltakstypeNavn: 'Varig tilrettelagt arbeid i ordinær virksomhet',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '3',
    tiltakstypeNavn: 'Inkluderingstilskudd',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '3',
    tiltakstypeNavn: 'Funksjonsassistanse i arbeidslivet',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '3',
    tiltakstypeNavn: 'Mentor',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '3',
    tiltakstypeNavn: 'Arbeidsrettet rehabilitering',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '3',
    tiltakstypeNavn: 'Individuell jobbstøtte',
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: '4',
    tiltakstypeNavn: 'Tilskudd til sommerjobb',
  },
];
