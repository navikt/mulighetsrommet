import { useQuery } from 'react-query';
import { Tiltaksgjennomforing } from 'mulighetsrommet-api-client';
import { QueryKeys } from '../../core/api/QueryKeys';
import { client } from '../../sanityClient';

export default function useTiltaksgjennomforingById(id: number) {
  return useQuery<Tiltaksgjennomforing[]>([QueryKeys.Tiltaksgjennomforinger, id], () =>
    client.fetch(`*[_type == "tiltaksgjennomforing" && tiltaksnummer == ${id}]{ 
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
        tiltakstype->}`)
  );
}
