import { useQuery } from 'react-query';
import { MulighetsrommetService, Tiltaksvariant } from '../../api';
import { QueryKeys } from '../../api/QueryKeys';

export default function useTiltaksvarianter() {
  return useQuery<Tiltaksvariant[]>(QueryKeys.Tiltaksvarianter, MulighetsrommetService.getTiltaksvarianter);
}
