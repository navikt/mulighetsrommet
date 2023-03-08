import { useQuery } from 'react-query';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';

export default function useLokasjonerForBruker() {
  return useQuery(QueryKeys.sanity.lokasjoner, () =>
    mulighetsrommetClient.sanity.getLokasjonerForBrukersEnhetOgFylke()
  );
}
