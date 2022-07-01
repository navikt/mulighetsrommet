import { Tiltaksgjennomforing } from '../models';
import { useGetTiltaksnummerFraUrl } from './useGetTiltaksnummerFraUrl';
import { useSanity } from './useSanity';

export default function useTiltaksgjennomforingByTiltaksnummer() {
  const tiltaksnummer = useGetTiltaksnummerFraUrl();
  return useSanity<Tiltaksgjennomforing>(`*[_type == "tiltaksgjennomforing" && !(_id in path("drafts.**")) && tiltaksnummer == ${tiltaksnummer}] {
    _id,
    tiltaksgjennomforingNavn,
    beskrivelse,
    tiltaksnummer,
    lokasjon,
    oppstart,
    oppstartsdato,
    faneinnhold {
      forHvemInfoboks,
      forHvem,
      detaljerOgInnholdInfoboks,
      detaljerOgInnhold,
      pameldingOgVarighetInfoboks,
      pameldingOgVarighet,
    },
    kontaktinfoArrangor->,
    kontaktinfoTiltaksansvarlige[]->,
    tiltakstype->{
      ...,
      regelverkFiler[]-> {
        _id,
        "regelverkFilUrl": regelverkFilOpplastning.asset->url,
        regelverkFilNavn
      },
      regelverkLenker[]->,
      innsatsgruppe->
    }
  }[0]`);
}
