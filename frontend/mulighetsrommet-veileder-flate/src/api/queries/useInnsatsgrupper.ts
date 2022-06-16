import { useQuery } from 'react-query';
import { Innsatsgruppe, MulighetsrommetService } from 'mulighetsrommet-api-client';
import { QueryKeys } from '../../core/api/QueryKeys';

export function useInnsatsgrupper() {
  return useQuery<Innsatsgruppe[]>(QueryKeys.Innsatsgrupper, MulighetsrommetService.getInnsatsgrupper);
}
