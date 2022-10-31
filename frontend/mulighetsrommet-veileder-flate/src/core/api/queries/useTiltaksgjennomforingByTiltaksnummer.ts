import groq from 'groq';
import { Tiltaksgjennomforing } from '../models';
import { useGetTiltaksgjennomforingIdFraUrl } from './useGetTiltaksgjennomforingIdFraUrl';
import { useSanity } from './useSanity';
import { useGetQueryParam } from '../../../hooks/useGetQueryParam';

export default function useTiltaksgjennomforingByTiltaksnummer() {
  const tiltaksgjennomforingId = useGetTiltaksgjennomforingIdFraUrl().replace('drafts.', '');
  const preview = useGetQueryParam('preview') === 'true';
  const matchIdForProdEllerDrafts = `(_id == '${tiltaksgjennomforingId}' || _id == 'drafts.${tiltaksgjennomforingId}')`;

  const response = useSanity<Tiltaksgjennomforing[]>(
    groq`*[_type == "tiltaksgjennomforing" && ${matchIdForProdEllerDrafts}] {
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
  }`,
    {
      includeUserdata: false,
    }
  );

  if (!response.data) {
    return response;
  }
  return { ...response, data: filterDataToSingleItem(response.data, preview) };
}

function filterDataToSingleItem(data: Tiltaksgjennomforing | Tiltaksgjennomforing[], preview: boolean) {
  if (!Array.isArray(data)) {
    return data;
  }

  if (data.length === 1) {
    return data[0];
  }

  if (preview) {
    return data.find(item => item._id.startsWith(`drafts.`)) || data[0];
  }

  return data[0];
}
