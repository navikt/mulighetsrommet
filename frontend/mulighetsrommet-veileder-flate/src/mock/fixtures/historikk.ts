import { faker } from '@faker-js/faker';
import { HistorikkForBruker } from 'mulighetsrommet-api-client';

export const historikk: HistorikkForBruker[] = genererHistorikk(7);

function genererHistorikk(antallRader: number): HistorikkForBruker[] {
  const data = [...Array(antallRader)].map(i => ({
    id: faker.git.shortSha(),
    fnr: '12345678910',
    fraDato: faker.date.recent(10).toString(),
    tilDato: faker.date.soon(10).toString(),
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

  return data as HistorikkForBruker[];
}
