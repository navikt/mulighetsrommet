import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { ArrangorService } from "@tiltaksadministrasjon/api-client";

export function useArrangorBetalingsinformasjon(id: string) {
  return useSuspenseQuery({
    queryKey: QueryKeys.arrangorBetalingsinfo(id),
    queryFn: async () => {
      const result = await ArrangorService.getBetalingsinformasjon({ path: { id } });
      return result.data ?? null;
    },
  });
}
