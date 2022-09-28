import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';
import { useQuery } from 'react-query';
import { DelMedBruker } from '../../../../../mulighetsrommet-api-client';
import { QueryKeys } from '../query-keys';
import { mulighetsrommetClient } from '../clients';

export function useHentTiltaksgjennomforingerDeltMedBruker() {
  const fnr = useHentFnrFraUrl();
  return useQuery<DelMedBruker[], any>([QueryKeys.DeltMedBrukerListe, fnr], () =>
    mulighetsrommetClient.delMedBruker.getTiltaksgjennomforingerDeltMedBruker({ fnr })
  );
}
