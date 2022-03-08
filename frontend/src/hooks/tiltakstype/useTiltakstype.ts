import { useQuery } from 'react-query';
import { MulighetsrommetService, Tiltakstype } from '../../api';
import { QueryKeys } from '../../core/api/QueryKeys';

export default function useTiltakstype(tiltakskode: string) {
  return useQuery<Tiltakstype>([QueryKeys.Tiltakstyper, tiltakskode], () =>
    MulighetsrommetService.getTiltakstype({ tiltakskode })
  );
}
