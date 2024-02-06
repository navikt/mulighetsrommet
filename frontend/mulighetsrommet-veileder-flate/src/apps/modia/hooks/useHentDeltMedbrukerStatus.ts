import { VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "@/core/api/clients";
import { QueryKeys } from "@/core/api/query-keys";
import { useHentVeilederdata } from "@/apps/modia/hooks/useHentVeilederdata";
import { useQuery } from "@tanstack/react-query";

export function useHentDeltMedBrukerStatus(norskIdent: string, gjennomforingId: string) {
  const { data: veilederData } = useHentVeilederdata();

  const { data: delMedBrukerInfo, refetch: refetchDelMedBruker } = useQuery({
    queryKey: [QueryKeys.DeltMedBrukerStatus, norskIdent, gjennomforingId],
    queryFn: async () => {
      const result = await mulighetsrommetClient.delMedBruker.getDelMedBruker({
        requestBody: { norskIdent, id: gjennomforingId },
      });
      return result || null; // Returner null hvis API returnerer 204 No Content = undefined;
    },
  });

  async function lagreVeilederHarDeltTiltakMedBruker(
    dialogId: string,
    gjennomforing: VeilederflateTiltaksgjennomforing,
  ) {
    if (!veilederData?.navIdent) return;

    const requestBody = {
      norskIdent,
      navident: veilederData.navIdent,
      sanityId: gjennomforing.sanityId,
      tiltaksgjennomforingId: gjennomforing.id,
      dialogId,
    };

    await mulighetsrommetClient.delMedBruker.delMedBruker({ requestBody });

    await refetchDelMedBruker();
  }

  return { delMedBrukerInfo, lagreVeilederHarDeltTiltakMedBruker };
}
