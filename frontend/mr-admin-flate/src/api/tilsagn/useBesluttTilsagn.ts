import { useMutation } from "@tanstack/react-query";
import { BesluttTilsagnRequest, ProblemDetail, TilsagnService } from "@mr/api-client-v2";
import { QueryKeys } from "../../api/QueryKeys";

export function useBesluttTilsagn() {
  return useMutation<unknown, ProblemDetail, { id: string; body: BesluttTilsagnRequest }>({
    mutationFn: ({ id, body }: { id: string; body: BesluttTilsagnRequest }) =>
      TilsagnService.besluttTilsagn({ path: { id }, body }),
    mutationKey: QueryKeys.besluttTilsagn(),
  });
}
