import { DelMedBruker } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { erPreview } from '../../../utils/Utils';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';
import { useHentVeilederdata } from './useHentVeilederdata';

export function useHentDeltMedBrukerStatus(sanityId: string | undefined, norskIdent: string) {
  const { data: veilederData } = useHentVeilederdata();

  const requestBody = {
    norskIdent,
    sanityId: sanityId ?? '',
  };

  const { data: sistDeltMedBruker, refetch: refetchDelMedBruker } = useQuery<DelMedBruker>(
    [QueryKeys.DeltMedBrukerStatus, norskIdent, sanityId],
    () => mulighetsrommetClient.delMedBruker.getDelMedBruker({ requestBody }),
    { enabled: !erPreview && !!sanityId }
  );

  async function lagreVeilederHarDeltTiltakMedBruker(dialogId: string, sanityId: string) {
    if (!veilederData?.navIdent) return;

    const requestBody = {
      norskIdent,
      navident: veilederData?.navIdent,
      sanityId,
      dialogId,
    };

    await mulighetsrommetClient.delMedBruker.delMedBruker({ requestBody });

    await refetchDelMedBruker();
  }

  return { harDeltMedBruker: sistDeltMedBruker, lagreVeilederHarDeltTiltakMedBruker };
}
