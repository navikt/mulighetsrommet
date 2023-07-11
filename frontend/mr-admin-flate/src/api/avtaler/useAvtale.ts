import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useAvtale(avtaleId?: string) {
  const enabled = !!avtaleId;

  return useQuery(
    QueryKeys.avtale(avtaleId!!),
    () => mulighetsrommetClient.avtaler.getAvtale({ id: avtaleId!! }),
    { enabled },
  );
}
