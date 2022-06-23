import { useAtom } from 'jotai';
import { Tiltaksgjennomforingsfilter, tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { Tiltaksgjennomforing } from '../models';
import { useSanity } from './useSanity';

export default function useTiltaksgjennomforing() {
  const [filter] = useAtom(tiltaksgjennomforingsfilter);
  return useSanity<Tiltaksgjennomforing[]>(`*[_type == "tiltaksgjennomforing" ${byggInnsatsgruppeFilter(filter)}]{
    _id,
    tiltaksgjennomforingNavn,
    lokasjon,
    oppstart,
    oppstartsdato,
    tiltaksnummer,
    kontaktinfoArrangor->,
    tiltakstype->
  }`);
}

function byggInnsatsgruppeFilter(filter: Tiltaksgjennomforingsfilter): string {
  if (filter.innsatsgrupper.length > 0) {
    const query = `&& tiltakstype->innsatsgruppe->tittel in [${filter.innsatsgrupper
      .map(gruppe => `"${gruppe.tittel}"`)
      .join(', ')}]`;
    return query;
  }

  return '';
}
