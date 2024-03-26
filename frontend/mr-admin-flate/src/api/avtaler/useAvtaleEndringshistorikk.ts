import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";

export function useAvtaleEndringshistorikk(id: string) {
  return useSuspenseQuery({
    queryKey: QueryKeys.avtaleHistorikk(id),
    queryFn() {
      return mulighetsrommetClient.avtaler.getAvtaleEndringshistorikk({
        id,
      });
    },
  });
}
