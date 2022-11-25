import { Ansatt } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { erPreview } from '../../../utils/Utils';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';

export function useHentVeilederdata() {
  return useQuery<Ansatt, Error>([QueryKeys.Veilederdata], () => mulighetsrommetClient.ansatt.hentInfoOmAnsatt(), {
    enabled: !erPreview,
  });
}
