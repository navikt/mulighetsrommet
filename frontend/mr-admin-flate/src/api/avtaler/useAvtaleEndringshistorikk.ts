import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

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
