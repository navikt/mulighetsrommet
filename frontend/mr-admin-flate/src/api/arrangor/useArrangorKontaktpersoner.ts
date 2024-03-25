import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

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
