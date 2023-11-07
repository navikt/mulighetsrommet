import { useQuery } from "@tanstack/react-query";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useAvtale(overstyrAvtaleId?: string) {
  const avtaleIdFromUrl = useGetAvtaleIdFromUrl();
  const avtaleId = overstyrAvtaleId ?? avtaleIdFromUrl;

  return useQuery({
    queryKey: QueryKeys.avtale(avtaleId!!),
    queryFn: () => mulighetsrommetClient.avtaler.getAvtale({ id: avtaleId! }),
    enabled: !!avtaleId,
  });
}
