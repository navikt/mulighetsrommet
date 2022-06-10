import { Tiltaksgjennomforing } from 'mulighetsrommet-api-client';
import { useSanity } from '../useSanity';

export default function useTiltaksgjennomforing() {
  return useSanity<Tiltaksgjennomforing[]>(`*[_type == "tiltaksgjennomforing"]{
    _id,
    tiltaksgjennomforingNavn,
    enheter,
    lokasjon,
    oppstart,
    oppstartsdato,
    tiltaksnummer,
    kontaktinfoArrangor->,
    tiltakstype->
    }`);
}
