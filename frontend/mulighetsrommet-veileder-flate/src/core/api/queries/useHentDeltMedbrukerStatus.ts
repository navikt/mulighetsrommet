import { DelMedBruker } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { useFnr } from '../../../hooks/useFnr';
import { erPreview } from '../../../utils/Utils';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';
import { useHentVeilederdata } from './useHentVeilederdata';
import useTiltaksgjennomforingById from './useTiltaksgjennomforingById';

export function useHentDeltMedBrukerStatus() {
  const { data: tiltaksgjennomforing } = useTiltaksgjennomforingById();
  const { data: veilederData } = useHentVeilederdata();
  const norskIdent = useFnr();

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
    if (!veilederData?.navIdent) return;

    await mulighetsrommetClient.delMedBruker.postDelMedBruker({
      requestBody: { norskIdent, navident: veilederData?.navIdent, sanityId, dialogId },
    });

    await refetchDelMedBruker();
  }

  return { harDeltMedBruker: sistDeltMedBruker, lagreVeilederHarDeltTiltakMedBruker };
}
