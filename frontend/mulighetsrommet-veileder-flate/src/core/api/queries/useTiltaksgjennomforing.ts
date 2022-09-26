import groq from 'groq';
import { useAtom } from 'jotai';
import { tiltaksgjennomforingsfilter, Tiltaksgjennomforingsfiltergruppe } from '../../atoms/atoms';
import { InnsatsgruppeNokler, Tiltaksgjennomforing } from '../models';
import { useHentBrukerdata } from './useHentBrukerdata';
import { useSanity } from './useSanity';

export default function useTiltaksgjennomforing() {
  const [filter] = useAtom(tiltaksgjennomforingsfilter);
  const brukerData = useHentBrukerdata();

  const sanityQueryString = groq`*[_type == "tiltaksgjennomforing" && !(_id in path("drafts.**")) 
  ${byggInnsatsgruppeFilter(filter.innsatsgruppe?.nokkel)} 
  ${byggTiltakstypeFilter(filter.tiltakstyper)}
  ${byggSokefilter(filter.search)}
  ${byggTiltaksgruppeFilterStreng(filter.tiltaksgruppe ?? [])}
  ${byggLokasjonsFilter(filter.lokasjoner ?? [])}
  ${byggEnhetOgFylkeFilter()}
  ]
  {
    _id,
    tiltaksgjennomforingNavn,
    lokasjon,
    oppstart,
    oppstartsdato,
    tiltaksnummer,
    kontaktinfoArrangor->{selskapsnavn},
    tiltakstype->{tiltakstypeNavn},
    tilgjengelighetsstatus
  }`;

  return useSanity<Tiltaksgjennomforing[]>(sanityQueryString, {
    enabled: !!brukerData.data?.oppfolgingsenhet,
  });
}

function byggEnhetOgFylkeFilter(): string {
  return groq`&& ($enhetsId in enheter[]._ref || (enheter[0] == null && $fylkeId == fylke._ref))`;
}

function byggLokasjonsFilter(lokasjoner: Tiltaksgjennomforingsfiltergruppe<string>[]): string {
  if (lokasjoner.length === 0) return '';

  const lokasjonsStreng = idSomListe(lokasjoner);

  return groq`&& lokasjon in [${lokasjonsStreng}]`;
}

function byggTiltaksgruppeFilterStreng(tiltaksgruppe: Tiltaksgjennomforingsfiltergruppe<string>[]): string {
  if (tiltaksgruppe.length === 0) return '';

  const tiltaksgruppeStreng = idSomListe(tiltaksgruppe);

  return groq`&& tiltakstype->tiltaksgruppe in [${tiltaksgruppeStreng}]`;
}

function byggInnsatsgruppeFilter(innsatsgruppe?: InnsatsgruppeNokler): string {
  if (!innsatsgruppe) return '';

  const innsatsgrupperISok = utledInnsatsgrupperFraInnsatsgruppe(innsatsgruppe)
    .map(nokkel => `"${nokkel}"`)
    .join(', ');
  return groq`&& tiltakstype->innsatsgruppe->nokkel in [${innsatsgrupperISok}]`;
}

function utledInnsatsgrupperFraInnsatsgruppe(innsatsgruppe: InnsatsgruppeNokler): InnsatsgruppeNokler[] {
  switch (innsatsgruppe) {
    case 'STANDARD_INNSATS':
      return ['STANDARD_INNSATS'];
    case 'SITUASJONSBESTEMT_INNSATS':
      return ['STANDARD_INNSATS', 'SITUASJONSBESTEMT_INNSATS'];
    case 'SPESIELT_TILPASSET_INNSATS':
      return ['STANDARD_INNSATS', 'SITUASJONSBESTEMT_INNSATS', 'SPESIELT_TILPASSET_INNSATS'];
    case 'VARIG_TILPASSET_INNSATS':
      return ['STANDARD_INNSATS', 'SITUASJONSBESTEMT_INNSATS', 'SPESIELT_TILPASSET_INNSATS', 'VARIG_TILPASSET_INNSATS'];
  }
}

function byggTiltakstypeFilter(tiltakstyper: Tiltaksgjennomforingsfiltergruppe<string>[]): string {
  return tiltakstyper.length > 0 ? groq`&& tiltakstype->_id in [${idSomListe(tiltakstyper)}]` : '';
}

function byggSokefilter(search: string | undefined) {
  return search
    ? groq`&& [tiltaksgjennomforingNavn, string(tiltaksnummer), tiltakstype->tiltakstypeNavn, lokasjon, kontaktinfoArrangor->selskapsnavn, oppstartsdato] match "*${search}*"`
    : '';
}

function idSomListe(elementer: Tiltaksgjennomforingsfiltergruppe<string>[]): string {
  return elementer.map(({ id }) => `"${id}"`).join(', ');
}
