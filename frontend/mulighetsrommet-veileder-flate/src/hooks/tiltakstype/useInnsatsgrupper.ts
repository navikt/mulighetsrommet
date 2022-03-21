import { useQuery } from 'react-query';
import { MulighetsrommetService, Innsatsgruppe } from 'mulighetsrommet-api';
import { QueryKeys } from '../../api/QueryKeys';

export function useInnsatsgrupper() {
  return useQuery<Innsatsgruppe[]>(QueryKeys.Innsatsgrupper, MulighetsrommetService.getInnsatsgrupper);
}
