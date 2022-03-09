import { useQuery } from 'react-query';
import { MulighetsrommetService, Tiltaksgjennomforing, Tiltakskode } from '../../api';
import { QueryKeys } from '../../core/api/QueryKeys';

export default function useTiltaksgjennomforingerByTiltakskode(tiltakskode: Tiltakskode) {
  return useQuery<Tiltaksgjennomforing[]>([QueryKeys.Tiltaksgjennomforinger, { tiltakskode }], () =>
    MulighetsrommetService.getTiltaksgjennomforingerByTiltakskode({ tiltakskode })
  );
}
