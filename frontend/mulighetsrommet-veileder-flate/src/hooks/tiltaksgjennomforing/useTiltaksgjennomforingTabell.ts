import { useQuery } from 'react-query';
import { Tiltaksgjennomforing } from 'mulighetsrommet-api-client';
import { QueryKeys } from '../../core/api/QueryKeys';
import { sanityClient } from '../../sanityClient';

export default function useTiltaksgjennomforingTabell() {
  return useQuery<Tiltaksgjennomforing>([QueryKeys.Tiltaksgjennomforinger], () =>
    sanityClient.fetch(
      `*[_type == "tiltaksgjennomforing"]{
        _id,
        tiltaksgjennomforingNavn,
        enheter,
        lokasjon,
        oppstart,
        oppstartsdato,
        tiltaksnummer,
        kontaktinfoArrangor->,
        tiltakstype->
        }`
    )
  );
}
