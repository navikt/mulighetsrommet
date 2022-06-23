import { useSanity } from './useSanity';
import { Tiltaksgjennomforing } from '../models';

export default function useTiltaksgjennomforingById(id: number) {
  return useSanity<Tiltaksgjennomforing>(`*[_type == "tiltaksgjennomforing" && tiltaksnummer == ${id}] {
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
    tiltakstype->{..., innsatsgruppe->}
  }[0]`);
}
