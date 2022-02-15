import { useQuery } from 'react-query';
import { MulighetsrommetService, Tiltakstype } from '../../api';
import { QueryKeys } from '../../api/QueryKeys';

export default function useTiltakstype(id: number) {
  return useQuery<Tiltakstype>([QueryKeys.Tiltakstyper, id], () => MulighetsrommetService.getTiltakstype({ id }));
}
