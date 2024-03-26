import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";

export const useArrangorKontaktpersoner = (arrangorId?: string) => {
  return useQuery({
    queryKey: QueryKeys.arrangorKontaktpersoner(arrangorId ?? ""),

    queryFn: () =>
      mulighetsrommetClient.arrangor.getArrangorKontaktpersoner({
        id: arrangorId!,
      }),

    enabled: !!arrangorId,
  });
};
