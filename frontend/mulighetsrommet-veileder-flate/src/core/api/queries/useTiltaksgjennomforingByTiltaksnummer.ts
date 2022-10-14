import groq from 'groq';
import { Tiltaksgjennomforing } from '../models';
import { useGetTiltaksgjennomforingIdFraUrl } from './useGetTiltaksnummerFraUrl';
import { useSanity } from './useSanity';

export default function useTiltaksgjennomforingByTiltaksnummer() {
  const tiltaksgjennomforingId = useGetTiltaksgjennomforingIdFraUrl();
  return useSanity<Tiltaksgjennomforing>(
    groq`*[_type == "tiltaksgjennomforing" && _id == '${tiltaksgjennomforingId}' && !(_id in path("drafts.**"))] {
    _id,
    tiltaksgjennomforingNavn,
    beskrivelse,
    "tiltaksnummer": tiltaksnummer.current,
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
      innsatsgruppe->,
    }
  }[0]`,
    {
      includeUserdata: false,
    }
  );
}
