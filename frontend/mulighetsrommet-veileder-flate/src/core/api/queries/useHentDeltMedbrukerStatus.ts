import { VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import { erPreview } from "../../../utils/Utils";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import { useHentVeilederdata } from "./useHentVeilederdata";
import { useQuery } from "@tanstack/react-query";

export function useHentDeltMedBrukerStatus(
  norskIdent: string,
  tiltaksgjennomforing?: VeilederflateTiltaksgjennomforing,
) {
  const { data: veilederData } = useHentVeilederdata();
  const id = tiltaksgjennomforing?.id ?? tiltaksgjennomforing?.sanityId;

  const { data: delMedBrukerInfo, refetch: refetchDelMedBruker } = useQuery({
    queryKey: [QueryKeys.DeltMedBrukerStatus, norskIdent, id],
    queryFn: async () => {
      const result = await mulighetsrommetClient.delMedBruker.getDelMedBruker({
        requestBody: { norskIdent, id: id!! },
      });
      return result || null; // Returner null hvis API returnerer 204 No Content = undefined;
    },
    enabled: !erPreview() && !!id,
  });

  async function lagreVeilederHarDeltTiltakMedBruker(
    dialogId: string,
    tiltaksgjennomforing: VeilederflateTiltaksgjennomforing,
  ) {
    if (!veilederData?.navIdent) return;

    const requestBody = {
      norskIdent,
      navident: veilederData.navIdent,
      sanityId: tiltaksgjennomforing.sanityId,
      tiltaksgjennomforingId: tiltaksgjennomforing.id,
      dialogId,
    };

    await mulighetsrommetClient.delMedBruker.delMedBruker({ requestBody });

    await refetchDelMedBruker();
  }

  return { delMedBrukerInfo, lagreVeilederHarDeltTiltakMedBruker };
}
