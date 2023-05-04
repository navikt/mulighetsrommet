import { useQuery } from 'react-query';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';

export default function useLokasjonerForBruker() {
  const fnr = useHentFnrFraUrl();
  return useQuery(QueryKeys.sanity.lokasjoner, () =>
    mulighetsrommetClient.sanity.getLokasjonerForBrukersEnhetOgFylke({ fnr })
  );
}
