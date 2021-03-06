import groq from 'groq';
import { useAtom } from 'jotai';
import { tiltaksgjennomforingsfilter, Tiltaksgjennomforingsfiltergruppe } from '../../core/atoms/atoms';
import { Tiltaksgjennomforing } from '../models';
import { useSanity } from './useSanity';

export default function useTiltaksgjennomforing() {
  const [filter] = useAtom(tiltaksgjennomforingsfilter);
  return useSanity<Tiltaksgjennomforing[]>(
    groq`*[_type == "tiltaksgjennomforing" && !(_id in path("drafts.**")) 
  ${byggInnsatsgruppeFilter(filter.innsatsgrupper)} 
  ${byggTiltakstypeFilter(filter.tiltakstyper)}
  ${byggSokefilter(filter.search)} 
  %ENHET%
  ]
  {
    _id,
    tiltaksgjennomforingNavn,
    lokasjon,
    oppstart,
    oppstartsdato,
    tiltaksnummer,
    kontaktinfoArrangor->,
    tiltakstype->
  }`
  );
}

function byggInnsatsgruppeFilter(innsatsgrupper: Tiltaksgjennomforingsfiltergruppe[]): string {
  return innsatsgrupper.length > 0
    ? `&& tiltakstype->innsatsgruppe->tittel in [${innsatsgrupper.map(gruppe => `"${gruppe.tittel}"`).join(', ')}]`
    : '';
}

function byggTiltakstypeFilter(tiltakstyper: Tiltaksgjennomforingsfiltergruppe[]): string {
  return tiltakstyper.length > 0
    ? `&& tiltakstype->_id in [${tiltakstyper.map(type => `"${type.id}"`).join(', ')}]`
    : '';
}

function byggSokefilter(search: string | undefined) {
  return search
    ? `&& [tiltaksgjennomforingNavn, string(tiltaksnummer), tiltakstype->tiltakstypeNavn, lokasjon, kontaktinfoArrangor->selskapsnavn, oppstartsdato] match "*${search}*"`
    : '';
}
