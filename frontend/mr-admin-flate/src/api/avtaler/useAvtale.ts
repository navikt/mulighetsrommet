import { useQuery } from "@tanstack/react-query";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useAvtale(overstyrAvtaleId?: string) {
  const avtaleId = overstyrAvtaleId || useGetAvtaleIdFromUrl();

  return useQuery(
    QueryKeys.avtale(avtaleId!!),
    () => mulighetsrommetClient.avtaler.getAvtale({ id: avtaleId! }),
    { enabled: !!avtaleId },
  );
}
