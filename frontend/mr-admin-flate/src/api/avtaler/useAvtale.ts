import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";

export function useAvtale(overstyrAvtaleId?: string) {
  const avtaleId = overstyrAvtaleId || useGetAvtaleIdFromUrl();
  const enabled = !!avtaleId;

  return useQuery(
    QueryKeys.avtale(avtaleId!!),
    () => mulighetsrommetClient.avtaler.getAvtale({ id: avtaleId!! }),
    { enabled },
  );
}
