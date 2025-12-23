import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import { ProblemDetail, TilsagnService } from "@tiltaksadministrasjon/api-client";

export function useGodkjennTilsagn() {
  return useApiMutation<unknown, ProblemDetail, { id: string }>({
    mutationFn: ({ id }) => TilsagnService.godkjennTilsagn({ path: { id } }),
    mutationKey: QueryKeys.godkjennTilsagn(),
  });
}
