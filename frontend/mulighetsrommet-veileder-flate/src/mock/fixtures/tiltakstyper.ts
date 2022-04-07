import faker from 'faker';
import { Tiltakskode, Tiltakstype } from '../../../../mulighetsrommet-api-client';
export const tiltakstyper: Tiltakstype[] = [
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 1,
    navn: 'Opplæring',
    tiltakskode: Tiltakskode.ABIST,
    sanityId: null,
    fraDato: faker.date.future(1).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 1,
    navn: 'Funksjonsassistanse',
    tiltakskode: Tiltakskode.FUNKSJASS,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 1,
    navn: 'Utvidet oppfølging',
    tiltakskode: Tiltakskode.UTVOPPFOPL,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 1,
    navn: 'Avklaring',
    tiltakskode: Tiltakskode.AVKLARAG,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 1,
    navn: 'Arbeidsmarkedsopplæring (AMO)',
    tiltakskode: Tiltakskode.GRUPPEAMO,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 1,
    navn: 'Ekspertbistand',
    tiltakskode: Tiltakskode.EKSPEBIST,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 1,
    navn: 'Jobbklubb',
    tiltakskode: Tiltakskode.JOBBKLUBB,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 2,
    navn: 'Oppfølging',
    tiltakskode: Tiltakskode.ABOPPF,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 2,
    navn: 'Digital jobbklubb',
    tiltakskode: Tiltakskode.DIGIOPPARB,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 2,
    navn: 'Fag- og yrkesopplæring',
    tiltakskode: Tiltakskode.ENKFAGYRKE,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 2,
    navn: 'Arbeidstrening',
    tiltakskode: Tiltakskode.ARBTREN,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 2,
    navn: 'Arbeidsforberedende trening',
    tiltakskode: Tiltakskode.ARBRRHDAG,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 2,
    navn: 'Midlertidig lønnstilskudd',
    tiltakskode: Tiltakskode.MIDLONTIL,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 2,
    navn: 'Varig lønnstilskudd',
    tiltakskode: Tiltakskode.MIDLONTIL,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 3,
    navn: 'Varig tilrettelagt arbeid i skjermet virksomhet',
    tiltakskode: Tiltakskode.TILRTILSK,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 3,
    navn: 'Varig tilrettelagt arbeid i ordinær virksomhet',
    tiltakskode: Tiltakskode.TILRETTEL,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 3,
    navn: 'Inkluderingstilskudd',
    tiltakskode: Tiltakskode.ABTBOPPF,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 3,
    navn: 'Funksjonsassistanse i arbeidslivet',
    tiltakskode: Tiltakskode.INKLUTILS,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 3,
    navn: 'Mentor',
    tiltakskode: Tiltakskode.MENTOR,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 3,
    navn: 'Arbeidsrettet rehabilitering',
    tiltakskode: Tiltakskode.ARBRRHBAG,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 3,
    navn: 'Individuell jobbstøtte',
    tiltakskode: Tiltakskode.INDJOBSTOT,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
  {
    id: faker.datatype.number({ min: 100000, max: 999999 }),
    innsatsgruppe: 4,
    navn: 'Tilskudd til sommerjobb',
    tiltakskode: Tiltakskode.TILSJOBB,
    sanityId: null,
    fraDato: faker.date.future(2).toISOString(),
    tilDato: faker.date.future(2).toISOString(),
    createdBy: null,
    createdAt: null,
    updatedBy: null,
    updatedAt: null,
  },
];
