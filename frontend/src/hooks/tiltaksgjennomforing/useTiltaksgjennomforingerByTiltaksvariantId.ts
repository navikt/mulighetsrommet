import { useQuery } from 'react-query';
import { MulighetsrommetService, Tiltaksgjennomforing } from '../../api';
import { QueryKeys } from '../../api/QueryKeys';

export default function useTiltaksgjennomforingerByTiltaksvariantId(id: number) {
  return useQuery<Tiltaksgjennomforing[]>([QueryKeys.Tiltaksgjennomforinger, { tiltaksvariantId: id }], () =>
    MulighetsrommetService.getTiltaksgjennomforingerByTiltaksvariant({ id })
  );
}
