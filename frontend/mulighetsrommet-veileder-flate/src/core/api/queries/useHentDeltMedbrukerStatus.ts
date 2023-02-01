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
  const brukerFnr = useHentFnrFraUrl();

  const { data: sistDeltMedBruker, refetch: refetchDelMedBruker } = useQuery<DelMedBruker>(
    [QueryKeys.DeltMedBrukerStatus, brukerFnr, tiltaksgjennomforing?._id],
    () =>
      mulighetsrommetClient.delMedBruker.getDelMedBruker({
        fnr: brukerFnr,
        tiltaksnummer: tiltaksgjennomforing?.tiltaksnummer?.toString()!!,
      }),
    { enabled: !erPreview || !tiltaksgjennomforing?.tiltaksnummer }
  );

  async function lagreVeilederHarDeltTiltakMedBruker(dialogId: string, tiltaksnummer: string) {
    if (!veilederData?.ident) return;

    await mulighetsrommetClient.delMedBruker.postDelMedBruker({
      tiltaksnummer,
      requestBody: { bruker_fnr: brukerFnr, navident: veilederData?.ident, tiltaksnummer, dialogId },
    });

    await refetchDelMedBruker();
  }

  return { harDeltMedBruker: sistDeltMedBruker, lagreVeilederHarDeltTiltakMedBruker };
}
