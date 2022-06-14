import { useSanity } from './useSanity';
import { Tiltaksgjennomforing } from '../models';

export default function useTiltaksgjennomforing() {
  return useSanity<Tiltaksgjennomforing[]>(`*[_type == "tiltaksgjennomforing"]{
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
