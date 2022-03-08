import { useQuery } from 'react-query';
import { MulighetsrommetService, Tiltaksgjennomforing } from '../../api';
import { QueryKeys } from '../../core/api/QueryKeys';

export default function useTiltaksgjennomforingerByTiltakskode(tiltakskode: string) {
  return useQuery<Tiltaksgjennomforing[]>([QueryKeys.Tiltaksgjennomforinger, { tiltakskode }], () =>
    MulighetsrommetService.getTiltaksgjennomforingerByTiltakskode({ tiltakskode })
  );
}
