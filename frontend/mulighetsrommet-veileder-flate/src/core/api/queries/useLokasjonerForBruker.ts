import { useQuery } from 'react-query';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';
import { useFnr } from '../../../hooks/useFnr';

export default function useLokasjonerForBruker() {
  const fnr = useFnr();
  return useQuery(QueryKeys.sanity.lokasjoner, () =>
    mulighetsrommetClient.sanity.getLokasjonerForBrukersEnhetOgFylke({ fnr })
  );
}
