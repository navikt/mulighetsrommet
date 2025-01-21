import { useApiQuery } from "@/hooks/useApiQuery";
import { useGetAvtaleIdFromUrl } from "@/hooks/useGetAvtaleIdFromUrl";
import { QueryKeys } from "@/api/QueryKeys";
import { AvtalerService } from "@mr/api-client-v2";

export function useAvtale(overstyrAvtaleId?: string) {
  const avtaleIdFromUrl = useGetAvtaleIdFromUrl();
  const avtaleId = overstyrAvtaleId ?? avtaleIdFromUrl;

  const query = useApiQuery({
    queryKey: QueryKeys.avtale(avtaleId!),
    queryFn: () => AvtalerService.getAvtale({ path: { id: avtaleId! } }),
    enabled: !!avtaleId,
  });

  return { ...query, isLoading: !!avtaleId && query.isLoading };
}
