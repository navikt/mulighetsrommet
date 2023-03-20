import { faker } from '@faker-js/faker';
import { Innsatsgruppe, SanityInnsatsgruppe, SanityTiltaksgjennomforing } from 'mulighetsrommet-api-client';

export const tiltaksgjennomforinger: SanityTiltaksgjennomforing[] = [
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
    tiltakstype: {
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
    tiltaksgjennomforingNavn: 'Varig tilrettelagt arbeid - VTA',
    beskrivelse: faker.lorem.paragraph(3),
    tiltaksnummer: '111111',
    kontaktinfoArrangor: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
      selskapsnavn: 'SoloPolo AS',
      telefonnummer: faker.datatype.number({ min: 10000000, max: 99999999 }).toString(),
      epost: faker.internet.email(),
      adresse: faker.address.streetAddress(),
    },
    lokasjon: faker.address.city(),
    oppstart: SanityTiltaksgjennomforing.oppstart.DATO,
    oppstartsdato: faker.date.future().toString(),
    kontaktinfoTiltaksansvarlige: [
      {
        _id: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
        navn: 'Solo Polo',
        enhet: 'NAV Fredrikstad',
        telefonnummer: faker.datatype.number({ min: 10000000, max: 99999999 }).toString(),
        epost: faker.internet.email(),
      },
    ],
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
    tiltakstype: {
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
    tiltaksgjennomforingNavn: 'Varig tilrettelagt arbeid - VTA',
    beskrivelse: faker.lorem.paragraph(3),
    tiltaksnummer: '123456',
    kontaktinfoArrangor: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
      selskapsnavn: 'SoloPolo AS',
      telefonnummer: faker.datatype.number({ min: 10000000, max: 99999999 }).toString(),
      epost: faker.internet.email(),
      adresse: faker.address.streetAddress(),
    },
    lokasjon: faker.address.city(),
    oppstart: SanityTiltaksgjennomforing.oppstart.DATO,
    oppstartsdato: faker.date.future().toString(),
    kontaktinfoTiltaksansvarlige: [
      {
        _id: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
        navn: 'Solo Polo',
        enhet: 'NAV Fredrikstad',
        telefonnummer: faker.datatype.number({ min: 10000000, max: 99999999 }).toString(),
        epost: faker.internet.email(),
      },
    ],
  },
  {
    _id: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
    tiltakstype: {
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
    tiltaksgjennomforingNavn: 'Varig tilrettelagt arbeid - VTA',
    beskrivelse: faker.lorem.paragraph(3),
    tiltaksnummer: '112233',
    kontaktinfoArrangor: {
      _id: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
      selskapsnavn: 'SoloPolo AS',
      telefonnummer: faker.datatype.number({ min: 10000000, max: 99999999 }).toString(),
      epost: faker.internet.email(),
      adresse: faker.address.streetAddress(),
    },
    lokasjon: faker.address.city(),
    oppstart: SanityTiltaksgjennomforing.oppstart.DATO,
    oppstartsdato: faker.date.future().toString(),
    kontaktinfoTiltaksansvarlige: [
      {
        _id: faker.datatype.number({ min: 100000, max: 999999 }).toString(),
        navn: 'Solo Polo',
        enhet: 'NAV Fredrikstad',
        telefonnummer: faker.datatype.number({ min: 10000000, max: 99999999 }).toString(),
        epost: faker.internet.email(),
      },
    ],
  },
];
