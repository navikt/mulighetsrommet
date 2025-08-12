import {
  BesluttTotrinnskontrollRequest,
  ProblemDetail,
  UtbetalingService,
} from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useBesluttAvbrytelse() {
  return useApiMutation<
    unknown,
    ProblemDetail,
    { id: string; body: BesluttTotrinnskontrollRequest }
  >({
    mutationFn: ({ id, body }: { id: string; body: BesluttTotrinnskontrollRequest }) =>
      UtbetalingService.besluttAvbryt({ path: { id }, body }),
    mutationKey: QueryKeys.besluttTilsagn(),
  });
}
