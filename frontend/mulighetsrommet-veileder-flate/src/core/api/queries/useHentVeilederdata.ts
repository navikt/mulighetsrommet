import { Veileder } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';

export function useHentVeilederdata() {
  return useQuery<Veileder, Error>([QueryKeys.Veilederdata], () => mulighetsrommetClient.veileder.getVeilederData());
}
