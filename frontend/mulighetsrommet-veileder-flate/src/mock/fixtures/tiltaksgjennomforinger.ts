import faker from 'faker';
import { Tiltaksgjennomforing } from '../../api/models';

export const tiltaksgjennomforinger: Tiltaksgjennomforing[] = [
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    tiltaksgjennomforingNavn: 'Opplæring',
    beskrivelse: faker.lorem.paragraph(3),

    tiltaksnummer: faker.datatype.number({ min: 100000, max: 999999 }),
    tiltakstype: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }),
      innsatsgruppe: '',
      tiltakstypeNavn: '',
    },
    kontaktinfoArrangor: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }),
      adresse: '',
      epost: '',
      selskapsnavn: '',
      telefonnummer: '',
    },
    lokasjon: '',
    oppstart: '',
    kontaktinfoTiltaksansvarlige: [
      {
        _id: faker.datatype.number({ min: 100000, max: 999999 }),
        enhet: '',
        epost: '',
        navn: '',
        telefonnummer: '',
      },
    ],
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    tiltaksgjennomforingNavn: 'Opplæring',
    beskrivelse: faker.lorem.paragraph(3),

    tiltaksnummer: faker.datatype.number({ min: 100000, max: 999999 }),
    tiltakstype: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }),
      innsatsgruppe: '',
      tiltakstypeNavn: '',
    },
    kontaktinfoArrangor: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }),
      adresse: '',
      epost: '',
      selskapsnavn: '',
      telefonnummer: '',
    },
    lokasjon: '',
    oppstart: '',
    kontaktinfoTiltaksansvarlige: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }),
      enhet: '',
      epost: '',
      navn: '',
      telefonnummer: '',
    },
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    tiltaksgjennomforingNavn: 'Opplæring',
    beskrivelse: faker.lorem.paragraph(3),

    tiltaksnummer: faker.datatype.number({ min: 100000, max: 999999 }),
    tiltakstype: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }),
      innsatsgruppe: '',
      tiltakstypeNavn: '',
    },
    kontaktinfoArrangor: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }),
      adresse: '',
      epost: '',
      selskapsnavn: '',
      telefonnummer: '',
    },
    lokasjon: '',
    oppstart: '',
    kontaktinfoTiltaksansvarlige: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }),
      enhet: '',
      epost: '',
      navn: '',
      telefonnummer: '',
    },
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    tiltaksgjennomforingNavn: 'Opplæring',
    beskrivelse: faker.lorem.paragraph(3),

    tiltaksnummer: faker.datatype.number({ min: 100000, max: 999999 }),
    tiltakstype: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }),
      innsatsgruppe: '',
      tiltakstypeNavn: '',
    },
    kontaktinfoArrangor: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }),
      adresse: '',
      epost: '',
      selskapsnavn: '',
      telefonnummer: '',
    },
    lokasjon: '',
    oppstart: '',
    kontaktinfoTiltaksansvarlige: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }),
      enhet: '',
      epost: '',
      navn: '',
      telefonnummer: '',
    },
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }),
    tiltaksgjennomforingNavn: 'Opplæring',
    beskrivelse: faker.lorem.paragraph(3),

    tiltaksnummer: faker.datatype.number({ min: 100000, max: 999999 }),
    tiltakstype: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }),
      innsatsgruppe: '',
      tiltakstypeNavn: '',
    },
    kontaktinfoArrangor: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }),
      adresse: '',
      epost: '',
      selskapsnavn: '',
      telefonnummer: '',
    },
    lokasjon: '',
    oppstart: '',
    kontaktinfoTiltaksansvarlige: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }),
      enhet: '',
      epost: '',
      navn: '',
      telefonnummer: '',
    },
  },
];
