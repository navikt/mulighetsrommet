import { MulighetsrommetService } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { QueryKeys } from '../../core/api/QueryKeys';
import { useHentFnrFraUrl } from '../../hooks/useHentFnrFraUrl';

export function useHentBrukerdata() {
  const fnr = useHentFnrFraUrl();
  if (!fnr) return undefined;

  return useQuery([QueryKeys.Brukerdata, fnr], () => MulighetsrommetService.getBrukerdata({ fnr }), {
    enabled: !!fnr, // Ikke kjør spørringen hvis vi ikke har et fnr
  });
}
