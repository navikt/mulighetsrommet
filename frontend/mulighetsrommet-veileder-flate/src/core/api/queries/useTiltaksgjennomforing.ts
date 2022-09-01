import groq from 'groq';
import { useAtom } from 'jotai';
import { tiltaksgjennomforingsfilter, Tiltaksgjennomforingsfiltergruppe } from '../../atoms/atoms';
import { InnsatsgruppeNokler, Tiltaksgjennomforing } from '../models';
import { useHentBrukerdata } from './useHentBrukerdata';
import { useSanity } from './useSanity';

export default function useTiltaksgjennomforing() {
  const [filter] = useAtom(tiltaksgjennomforingsfilter);
  const brukerData = useHentBrukerdata();
  return useSanity<Tiltaksgjennomforing[]>(
    groq`*[_type == "tiltaksgjennomforing" && !(_id in path("drafts.**")) 
  ${byggInnsatsgruppeFilter(filter.innsatsgruppe?.nokkel)} 
  ${byggTiltakstypeFilter(filter.tiltakstyper)}
  ${byggSokefilter(filter.search)}
  && (($enhetsId in enheter[]->nummer.current) || (enheter[0] == null && $fylkeId == fylke->nummer.current))
  ]
  {
    _id,
    tiltaksgjennomforingNavn,
    lokasjon,
    oppstart,
    oppstartsdato,
    tiltaksnummer,
    kontaktinfoArrangor->,
    tiltakstype->,
    tilgjengelighetsstatus
  }`,
    {
      enabled: !!brukerData.data?.oppfolgingsenhet,
    }
  );
}

function byggInnsatsgruppeFilter(innsatsgruppe?: InnsatsgruppeNokler): string {
  if (!innsatsgruppe) return '';

  const innsatsgrupperISok = utledInnsatsgrupperFraInnsatsgruppe(innsatsgruppe)
    .map(nokkel => `"${nokkel}"`)
    .join(', ');
  return `&& tiltakstype->innsatsgruppe->nokkel in [${innsatsgrupperISok}]`;
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
  return tiltakstyper.length > 0
    ? `&& tiltakstype->_id in [${tiltakstyper.map(type => `"${type.id}"`).join(', ')}]`
    : '';
}

function byggSokefilter(search: string | undefined) {
  return search
    ? `&& [tiltaksgjennomforingNavn, string(tiltaksnummer), tiltakstype->tiltakstypeNavn, lokasjon, kontaktinfoArrangor->selskapsnavn, oppstartsdato] match "*${search}*"`
    : '';
}
