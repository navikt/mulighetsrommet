import {
  AarsakerOgForklaringRequestTilsagnStatusAarsak,
  TilsagnService,
  ProblemDetail,
} from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useTilsagnTilAnnullering() {
  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; request: AarsakerOgForklaringRequestTilsagnStatusAarsak }
  >({
    mutationFn: ({ id, request }) => TilsagnService.tilAnnullering({ path: { id }, body: request }),
    mutationKey: QueryKeys.annullerTilsagn(),
  });
}
