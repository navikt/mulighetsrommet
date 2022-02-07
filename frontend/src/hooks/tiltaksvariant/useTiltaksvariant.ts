import { useQuery } from 'react-query';
import { MulighetsrommetService, Tiltaksvariant } from '../../api';
import { QueryKeys } from '../../api/QueryKeys';

export default function useTiltaksvariant(id: number) {
  return useQuery<Tiltaksvariant>([QueryKeys.Tiltaksvarianter, id], () =>
    MulighetsrommetService.getTiltaksvariant({ id })
  );
}
