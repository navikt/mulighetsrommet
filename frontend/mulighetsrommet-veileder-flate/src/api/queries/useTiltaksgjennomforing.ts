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
  return (
    (filter.innsatsgrupper.length > 0
      ? `&& tiltakstype->innsatsgruppe->tittel in [${filter.innsatsgrupper
          .map(gruppe => `"${gruppe.tittel}"`)
          .join(', ')}]`
      : '') +
    (filter.tiltakstyper.length > 0
      ? `&& tiltakstype->_id in [${filter.tiltakstyper.map(type => `"${type.id}"`).join(', ')}]`
      : '')
  );
}
