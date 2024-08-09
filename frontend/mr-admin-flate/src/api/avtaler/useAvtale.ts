import { useQuery } from "@tanstack/react-query";
import { useGetAvtaleIdFromUrl } from "@/hooks/useGetAvtaleIdFromUrl";
import { QueryKeys } from "@/api/QueryKeys";
import { AvtalerService } from "mulighetsrommet-api-client";

export function useAvtale(overstyrAvtaleId?: string) {
  const avtaleIdFromUrl = useGetAvtaleIdFromUrl();
  const avtaleId = overstyrAvtaleId ?? avtaleIdFromUrl;

  const query = useQuery({
    queryKey: QueryKeys.avtale(avtaleId!!),
    queryFn: () => AvtalerService.getAvtale({ id: avtaleId! }),
    enabled: !!avtaleId,
  });

  return { ...query, isLoading: !!avtaleId && query.isLoading };
}
