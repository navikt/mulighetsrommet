import { Bruker } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { QueryKeys } from '../query-keys';
import { useFnr } from '../../../hooks/useFnr';
import { mulighetsrommetClient } from '../clients';
import { erPreview } from '../../../utils/Utils';

export function useHentBrukerdata() {
  const fnr = useFnr();

  const requestBody = { norskIdent: fnr };

  return useQuery<Bruker, Error>(
    [QueryKeys.Brukerdata, fnr],
    () => mulighetsrommetClient.bruker.getBrukerdata({ requestBody }),
    { enabled: !erPreview }
  );
}
