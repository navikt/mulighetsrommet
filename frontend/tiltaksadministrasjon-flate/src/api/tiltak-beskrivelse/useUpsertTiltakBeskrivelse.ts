import {
  ProblemDetail,
  TiltakBeskrivelseRequest,
  TiltakBeskrivelseService,
} from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useUpsertTiltakBeskrivelse() {
  return useApiMutation<unknown, ProblemDetail, TiltakBeskrivelseRequest>({
    mutationFn: async (body: TiltakBeskrivelseRequest) => {
      return TiltakBeskrivelseService.upsertTiltakBeskrivelse({ body });
    },
  });
}
