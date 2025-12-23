import {
  AarsakerOgForklaringRequestTilsagnStatusAarsak,
  ProblemDetail,
  TilsagnService,
} from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "../QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useTilsagnTilOppgjor() {
  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; request: AarsakerOgForklaringRequestTilsagnStatusAarsak }
  >({
    mutationFn: ({ id, request }) => TilsagnService.gjorOpp({ path: { id }, body: request }),
    mutationKey: QueryKeys.gjorOppTilsagn(),
  });
}
