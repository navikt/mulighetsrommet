import { useApiQuery } from "@mr/frontend-common";
import { useGetAvtaleIdFromUrl } from "@/hooks/useGetAvtaleIdFromUrl";
import { QueryKeys } from "@/api/QueryKeys";
import { AvtalerService } from "@mr/api-client-v2";

export function useAvtale(overstyrAvtaleId?: string) {
  const avtaleIdFromUrl = useGetAvtaleIdFromUrl();
  const avtaleId = overstyrAvtaleId ?? avtaleIdFromUrl;

  return useApiQuery({
    queryKey: QueryKeys.avtale(avtaleId),
    queryFn: () => AvtalerService.getAvtale({ path: { id: avtaleId! } }),
    enabled: !!avtaleId,
  });
}
