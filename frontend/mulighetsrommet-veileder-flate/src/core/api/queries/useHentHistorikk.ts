import { useQuery } from 'react-query';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';

export function useHentHistorikk(prefetch: boolean = true) {
  const fnr = useHentFnrFraUrl();
  return useQuery([QueryKeys.Historikk, fnr], () => mulighetsrommetClient.historikk.hentHistorikkForBruker({ fnr }), {
    enabled: prefetch,
  });
}
