import { BesluttTilsagnRequest, ProblemDetail, TilsagnService } from "@mr/api-client-v2";
import { QueryKeys } from "../../api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";

export function useBesluttTilsagn() {
  return useApiMutation<unknown, ProblemDetail, { id: string; body: BesluttTilsagnRequest }>({
    mutationFn: ({ id, body }: { id: string; body: BesluttTilsagnRequest }) =>
      TilsagnService.besluttTilsagn({ path: { id }, body }),
    mutationKey: QueryKeys.besluttTilsagn(),
  });
}
