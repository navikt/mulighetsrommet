// TODO Type opp historikk basert på modell fra openapi.yaml
import { faker } from '@faker-js/faker';

interface Historikk {
  id: string;
  fnr: string;
  fraDato: Date;
  tilDato: Date;
  status: Deltakerstatus;
  tiltaksnummer: string;
  tiltaksnavn: string;
  tiltakstype: string;
  arrangor: string;
}

type Deltakerstatus = 'VENTER' | 'AVSLUTTET' | 'DELTAR' | 'IKKE_AKTUELL';

export const historikk: Historikk[] = genererHistorikk(7);

function genererHistorikk(antallRader: number): Historikk[] {
  const data = [...Array(antallRader)].map(i => ({
    id: faker.git.shortSha(),
    fnr: '12345678910',
    fraDato: faker.date.recent(10),
    tilDato: faker.date.soon(10),
    status: faker.helpers.arrayElement(['VENTER', 'AVSLUTTET', 'DELTAR', 'IKKE_AKTUELL']),
    tiltaksnummer: faker.random.numeric(6),
    tiltaksnavn: faker.company.catchPhrase(),
    tiltakstype: faker.helpers.arrayElement([
      'Lønnstilskudd',
      'Mentor',
      'Midlertidig lønnstilskudd',
      'Nettbasert arbeidsmarkedsopplæring (AMO)',
      'Nettkurs',
      '2-årig opplæringstiltak',
      'Arbeidspraksis i skjermet virksomhet',
      'Arbeidspraksis i ordinær virksomhet',
      'Produksjonsverksted (PV)',
      'Resultatbasert finansiering av oppfølging',
      'Spa prosjekter',
      'Lærlinger i statlige etater',
    ]),
    arrangor: faker.helpers.arrayElement(['AS3', 'Adecco', 'Jobbklubben', 'Kom i Arbeid AS']),
  }));

  return data as Historikk[];
}
