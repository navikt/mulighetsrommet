import { useQuery } from 'react-query';
import { MulighetsrommetService, Tiltaksgjennomforing } from '../../api';
import { QueryKeys } from '../../api/QueryKeys';

export default function useTiltaksgjennomforingerByTiltakstypeId(id: number) {
  return useQuery<Tiltaksgjennomforing[]>([QueryKeys.Tiltaksgjennomforinger, { tiltakstypeId: id }], () =>
    MulighetsrommetService.getTiltaksgjennomforingerByTiltakstype({ id })
  );
}
