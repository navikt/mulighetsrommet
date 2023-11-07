import { DelMedBruker, VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useQuery } from "react-query";
import { erPreview } from "../../../utils/Utils";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import { useHentVeilederdata } from "./useHentVeilederdata";

export function useHentDeltMedBrukerStatus(
  norskIdent: string,
  tiltaksgjennomforing?: VeilederflateTiltaksgjennomforing,
) {
  const { data: veilederData } = useHentVeilederdata();
  const id = tiltaksgjennomforing?.id ?? tiltaksgjennomforing?.sanityId;

  const { data: sistDeltMedBruker, refetch: refetchDelMedBruker } = useQuery<DelMedBruker>(
    [QueryKeys.DeltMedBrukerStatus, norskIdent, id],
    () =>
      mulighetsrommetClient.delMedBruker.getDelMedBruker({ requestBody: { norskIdent, id: id!! } }),
    { enabled: !erPreview() && !!id },
  );

  async function lagreVeilederHarDeltTiltakMedBruker(
    dialogId: string,
    tiltaksgjennomforing: VeilederflateTiltaksgjennomforing,
  ) {
    if (!veilederData?.navIdent) return;

    const requestBody = {
      norskIdent,
      navident: veilederData?.navIdent,
      sanityId: tiltaksgjennomforing.sanityId,
      tiltaksgjennomforingId: tiltaksgjennomforing.id,
      dialogId,
    };

    await mulighetsrommetClient.delMedBruker.delMedBruker({ requestBody });

    await refetchDelMedBruker();
  }

  return { harDeltMedBruker: sistDeltMedBruker, lagreVeilederHarDeltTiltakMedBruker };
}
