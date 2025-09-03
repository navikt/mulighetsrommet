import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import { ProblemDetail } from "@mr/api-client-v2";
import {
  BesluttTotrinnskontrollRequestTilsagnStatusAarsak,
  TilsagnService,
} from "@tiltaksadministrasjon/api-client";

export function useBesluttTilsagn() {
  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; body: BesluttTotrinnskontrollRequestTilsagnStatusAarsak }
  >({
    mutationFn: ({ id, body }) => TilsagnService.besluttTilsagn({ path: { id }, body }),
    mutationKey: QueryKeys.besluttTilsagn(),
  });
}
