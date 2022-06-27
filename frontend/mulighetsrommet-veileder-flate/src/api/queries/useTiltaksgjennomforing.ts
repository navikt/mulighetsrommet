import { useAtom } from 'jotai';
import {Tiltaksgjennomforingsfilter, tiltaksgjennomforingsfilter, Tiltaksgjenomforingsfiltergruppe} from '../../core/atoms/atoms';
import { Tiltaksgjennomforing } from '../models';
import { useSanity } from './useSanity';

export default function useTiltaksgjennomforing() {
  const [filter] = useAtom(tiltaksgjennomforingsfilter);
  return useSanity<Tiltaksgjennomforing[]>(`*[_type == "tiltaksgjennomforing" ${byggInnsatsgruppeFilter(filter.innsatsgrupper)} ${byggTiltakstypeFilter(filter.tiltakstyper)}]{
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

function byggInnsatsgruppeFilter(innsatsgrupper: Tiltaksgjenomforingsfiltergruppe[]): string {
  return innsatsgrupper.length > 0
      ? `&& tiltakstype->innsatsgruppe->tittel in [${innsatsgrupper
          .map(gruppe => `"${gruppe.tittel}"`)
          .join(', ')}]`
      : '';
}

function byggTiltakstypeFilter(tiltakstyper: Tiltaksgjenomforingsfiltergruppe[]): string {
  return tiltakstyper.length > 0
      ? `&& tiltakstype->_id in [${tiltakstyper.map(type => `"${type.id}"`).join(', ')}]`
      : '';
}
