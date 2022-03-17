import { useQuery } from 'react-query';
import { MulighetsrommetService, Tiltaksgjennomforing } from 'mulighetsrommet-api';
import { QueryKeys } from '../../core/api/QueryKeys';

export default function useTiltaksgjennomforing(id: number) {
  return useQuery<Tiltaksgjennomforing>([QueryKeys.Tiltaksgjennomforinger, id], () =>
    MulighetsrommetService.getTiltaksgjennomforing({ id })
  );
}
