import { useQuery } from 'react-query';
import { MulighetsrommetService } from '../../api';
import { Innsatsgruppe } from '../../api/models/Innsatsgruppe';
import { QueryKeys } from '../../core/api/QueryKeys';

export function useInnsatsgrupper() {
  return useQuery<Innsatsgruppe[]>(QueryKeys.Innsatsgrupper, MulighetsrommetService.getInnsatsgrupper);
}
