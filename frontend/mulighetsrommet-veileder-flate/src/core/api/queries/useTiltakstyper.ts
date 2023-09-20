import { useQuery } from 'react-query';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';

export function useTiltakstyper() {
  return useQuery(QueryKeys.sanity.tiltakstyper, () => mulighetsrommetClient.sanity.getVeilederflateTiltakstyper());
}
