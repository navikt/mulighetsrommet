import faker from 'faker';
import { Tiltaksvariant } from '../../core/domain/Tiltaksvariant';

export const tiltaksvarianter: Omit<Tiltaksvariant, 'id'>[] = [
  {
    innsatsgruppe: 1,
    tittel: 'Opplæring',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 1,
    tittel: 'Funksjonsassistanse',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 1,
    tittel: 'Utvidet oppfølging',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 1,
    tittel: 'Avklaring',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 1,
    tittel: 'Arbeidsmarkedsopplæring (AMO)',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 1,
    tittel: 'Ekspertbistand',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 1,
    tittel: 'Jobbklubb',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 2,
    tittel: 'Oppfølging',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 2,
    tittel: 'Digital jobbklubb',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 2,
    tittel: 'Fag- og yrkesopplæring',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 2,
    tittel: 'Arbeidstrening',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 2,
    tittel: 'Arbeidsforberedende trening',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 2,
    tittel: 'Midlertidig lønnstilskudd',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 2,
    tittel: 'Varig lønnstilskudd',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 3,
    tittel: 'Varig tilrettelagt arbeid i skjermet virksomhet',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 3,
    tittel: 'Varig tilrettelagt arbeid i ordinær virksomhet',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 3,
    tittel: 'Inkluderingstilskudd',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 3,
    tittel: 'Funksjonsassistanse i arbeidslivet',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 3,
    tittel: 'Mentor',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 3,
    tittel: 'Arbeidsrettet rehabilitering',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 3,
    tittel: 'Individuell jobbstøtte',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
  {
    innsatsgruppe: 4,
    tittel: 'Tilskudd til sommerjobb',
    beskrivelse: faker.lorem.paragraph(1),
    ingress: faker.lorem.sentence(10),
  },
];
