import { DelMedBruker } from "mulighetsrommet-api-client";
import { useQuery } from "react-query";
import { erPreview } from "../../../utils/Utils";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import { useHentVeilederdata } from "./useHentVeilederdata";

export function useHentDeltMedBrukerStatus(norskIdent: string, id?: string) {
  const { data: veilederData } = useHentVeilederdata();

  const { data: sistDeltMedBruker, refetch: refetchDelMedBruker } = useQuery<DelMedBruker>(
    [QueryKeys.DeltMedBrukerStatus, norskIdent, id],
    () =>
      mulighetsrommetClient.delMedBruker.getDelMedBruker({ requestBody: { norskIdent, id: id!! } }),
    { enabled: !erPreview() && !!id },
  );

  async function lagreVeilederHarDeltTiltakMedBruker(
    dialogId: string,
    tiltaksgjennomforingId?: string,
    sanityId?: string,
  ) {
    if (!veilederData?.navIdent) return;

    const requestBody = {
      norskIdent,
      navident: veilederData?.navIdent,
      sanityId,
      tiltaksgjennomforingId,
      dialogId,
    };

    await mulighetsrommetClient.delMedBruker.delMedBruker({ requestBody });

    await refetchDelMedBruker();
  }

  return { harDeltMedBruker: sistDeltMedBruker, lagreVeilederHarDeltTiltakMedBruker };
}
