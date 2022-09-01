import { Bruker } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { QueryKeys } from '../query-keys';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';
import { mulighetsrommetClient } from '../clients';

export function useHentBrukerdata() {
  const fnr = useHentFnrFraUrl();

  return useQuery<Bruker, Error>([QueryKeys.Brukerdata, fnr], () =>
    mulighetsrommetClient.bruker.getBrukerdata({ fnr })
  );
}
