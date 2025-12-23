import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import {
  AarsakerOgForklaringRequestTilsagnStatusAarsak,
  ProblemDetail,
  TilsagnService,
} from "@tiltaksadministrasjon/api-client";

export function useReturnerTilsagn() {
  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; request: AarsakerOgForklaringRequestTilsagnStatusAarsak }
  >({
    mutationFn: ({ id, request }) =>
      TilsagnService.returnerTilsagn({ path: { id }, body: request }),
    mutationKey: QueryKeys.returnerTilsagn(),
  });
}
