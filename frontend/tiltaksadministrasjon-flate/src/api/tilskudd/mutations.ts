import {
  HelVedSimuleringResponse,
  ProblemDetail,
  SimulerOpphorRequest,
  TilskuddService,
} from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useSimulerOpphorTilskuddVedtak() {
  return useApiMutation<HelVedSimuleringResponse, ProblemDetail, SimulerOpphorRequest>({
    mutationFn: async (body) => {
      const result = await TilskuddService.postTilskuddVedtakOpphorSimulering({ body });
      return result.data;
    },
  });
}
