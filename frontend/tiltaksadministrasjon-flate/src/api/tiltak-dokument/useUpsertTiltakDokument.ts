import {
  ProblemDetail,
  TiltakDokumentRequest,
  TiltakDokumentService,
} from "@tiltaksadministrasjon/api-client";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useUpsertTiltakDokument() {
  return useApiMutation<unknown, ProblemDetail, TiltakDokumentRequest>({
    mutationFn: async (body: TiltakDokumentRequest) => {
      return TiltakDokumentService.upsertTiltakDokument({ body });
    },
  });
}
