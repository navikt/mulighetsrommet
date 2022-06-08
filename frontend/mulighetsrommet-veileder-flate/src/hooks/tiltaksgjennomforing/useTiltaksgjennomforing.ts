import { useQuery } from 'react-query';
import { Tiltaksgjennomforing } from 'mulighetsrommet-api-client';
import { QueryKeys } from '../../core/api/QueryKeys';
import { client } from '../../sanityClient';

export default function useTiltaksgjennomforing() {
  return useQuery<Tiltaksgjennomforing[]>([QueryKeys.Tiltaksgjennomforinger], () =>
    client.fetch(
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
