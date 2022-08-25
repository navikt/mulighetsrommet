import { Bruker, MulighetsrommetService } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { QueryKeys } from '../query-keys';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';

export function useHentBrukerdata() {
  const fnr = useHentFnrFraUrl();

  return useQuery<Bruker, Error>([QueryKeys.Brukerdata, fnr], () => MulighetsrommetService.getBrukerdata({ fnr }));
}
