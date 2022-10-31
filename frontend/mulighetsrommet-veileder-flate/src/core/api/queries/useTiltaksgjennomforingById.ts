import groq from 'groq';
import { Tiltaksgjennomforing } from '../models';
import { useGetTiltaksgjennomforingIdFraUrl } from './useGetTiltaksgjennomforingIdFraUrl';
import { useSanity } from './useSanity';
import { erPreview } from '../../../utils/Utils';

export default function useTiltaksgjennomforingById() {
  const tiltaksgjennomforingId = useGetTiltaksgjennomforingIdFraUrl();
  const preview = erPreview;
  const ekskluderDrafts = preview ? '' : '&& !(_id in path("drafts.**"))';
  return useSanity<Tiltaksgjennomforing>(
    groq`*[_type == "tiltaksgjennomforing" && _id == '${tiltaksgjennomforingId}' ${ekskluderDrafts}] {
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
