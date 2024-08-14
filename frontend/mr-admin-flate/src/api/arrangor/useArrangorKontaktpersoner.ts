import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { ArrangorService } from "@mr/api-client";

export function useArrangorKontaktpersoner(arrangorId?: string) {
  return useQuery({
    queryKey: QueryKeys.arrangorKontaktpersoner(arrangorId ?? ""),

    queryFn: () =>
      ArrangorService.getArrangorKontaktpersoner({
        id: arrangorId!,
      }),

    enabled: !!arrangorId,
  });
}
