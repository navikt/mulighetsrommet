import { useQuery } from 'react-query';
import { MulighetsrommetService, Tiltakskode, Tiltakstype } from 'mulighetsrommet-api';
import { QueryKeys } from '../../core/api/QueryKeys';

export default function useTiltakstype(tiltakskode: Tiltakskode) {
  return useQuery<Tiltakstype>([QueryKeys.Tiltakstyper, tiltakskode], () =>
    MulighetsrommetService.getTiltakstype({ tiltakskode })
  );
}
