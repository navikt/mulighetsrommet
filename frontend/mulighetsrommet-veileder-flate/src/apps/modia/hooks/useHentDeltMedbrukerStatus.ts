import { DelMedBrukerService, VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import { QueryKeys } from "@/api/query-keys";
import { useHentVeilederdata } from "@/apps/modia/hooks/useHentVeilederdata";
import { useQuery } from "@tanstack/react-query";

export function useHentDeltMedBrukerStatus(norskIdent: string, gjennomforingId: string) {
  const { data: veilederData } = useHentVeilederdata();

  const { data: delMedBrukerInfo, refetch: refetchDelMedBruker } = useQuery({
    queryKey: [QueryKeys.DeltMedBrukerStatus, norskIdent, gjennomforingId],
    queryFn: async () => {
      const result = await DelMedBrukerService.getDelMedBruker({
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

    await DelMedBrukerService.delMedBruker({ requestBody });

    await refetchDelMedBruker();
  }

  return { delMedBrukerInfo, lagreVeilederHarDeltTiltakMedBruker };
}
