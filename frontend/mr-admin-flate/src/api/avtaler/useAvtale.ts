import { useQuery } from "@tanstack/react-query";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";

export function useAvtale(overstyrAvtaleId?: string) {
  const avtaleIdFromUrl = useGetAvtaleIdFromUrl();
  const avtaleId = overstyrAvtaleId ?? avtaleIdFromUrl;

  const query = useQuery({
    queryKey: QueryKeys.avtale(avtaleId!!),
    queryFn: () => mulighetsrommetClient.avtaler.getAvtale({ id: avtaleId! }),
    enabled: !!avtaleId,
  });

  return { ...query, isLoading: !!avtaleId && query.isLoading };
}
