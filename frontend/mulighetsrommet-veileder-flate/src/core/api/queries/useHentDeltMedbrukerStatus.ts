import { DelMedBruker } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';
import { erPreview } from '../../../utils/Utils';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';
import { useHentVeilederdata } from './useHentVeilederdata';
import useTiltaksgjennomforingById from './useTiltaksgjennomforingById';

export function useHentDeltMedBrukerStatus() {
  const { data: tiltaksgjennomforing } = useTiltaksgjennomforingById();
  const { data: veilederData } = useHentVeilederdata();
  const norskIdent = useHentFnrFraUrl();

  const { data: sistDeltMedBruker, refetch: refetchDelMedBruker } = useQuery<DelMedBruker>(
    [QueryKeys.DeltMedBrukerStatus, norskIdent, tiltaksgjennomforing?._id],
    () =>
      mulighetsrommetClient.delMedBruker.getDelMedBruker({
        fnr: norskIdent,
        sanityId: tiltaksgjennomforing?._id!!,
      }),
    { enabled: !erPreview || !tiltaksgjennomforing?.tiltaksnummer }
  );

  async function lagreVeilederHarDeltTiltakMedBruker(dialogId: string, sanityId: string) {
    if (!veilederData?.ident) return;

    await mulighetsrommetClient.delMedBruker.postDelMedBruker({
      sanityId,
      requestBody: { norskIdent, navident: veilederData?.ident, sanityId, dialogId },
    });

    await refetchDelMedBruker();
  }

  return { harDeltMedBruker: sistDeltMedBruker, lagreVeilederHarDeltTiltakMedBruker };
}
