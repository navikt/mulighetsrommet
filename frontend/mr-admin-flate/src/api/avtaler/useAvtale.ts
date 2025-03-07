import { QueryKeys } from "@/api/QueryKeys";
import { useGetAvtaleIdFromUrl } from "@/hooks/useGetAvtaleIdFromUrl";
import { AvtalerService } from "@mr/api-client-v2";
import { useApiQuery } from "@mr/frontend-common";

export function useAvtale(overstyrAvtaleId?: string) {
  const avtaleIdFromUrl = useGetAvtaleIdFromUrl();
  const avtaleId = overstyrAvtaleId ?? avtaleIdFromUrl;

  return useApiQuery({
    queryKey: QueryKeys.avtale(avtaleId),
    queryFn: async () => {
      return AvtalerService.getAvtale({ path: { id: avtaleId! } });
    },
    enabled: !!avtaleId,
  });
}
