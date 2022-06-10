import { Tiltaksgjennomforing } from 'mulighetsrommet-api-client';
import { useSanity } from '../useSanity';

export default function useTiltaksgjennomforingById(id: number) {
  return useSanity<Tiltaksgjennomforing[]>(`*[_type == "tiltaksgjennomforing" && tiltaksnummer == ${id}]{ 
        _id,
        tiltaksgjennomforingNavn,
        beskrivelse,
        tiltaksnummer,
        lokasjon,
        oppstart,
        oppstartsdato,
        enheter{
          fylke,
          asker,
          fredrikstad,
          indreOstfold,
          lillestrom,
          ringsaker,
          sarpsborg,
          skiptvedtMarker,
          steinkjer,
          trondheim
        },
        faneinnhold{
          forHvemInfoboks,
          forHvem,
          detaljerOgInnholdInfoboks,
          detaljerOgInnhold,
          pameldingOgVarighetInfoboks,
          pameldingOgVarighet,
        },
        kontaktinfoArrangor->,
        kontaktinfoTiltaksansvarlig->,
        tiltakstype->}`);
}
