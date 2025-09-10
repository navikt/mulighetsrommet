import { QueryKeys } from "@/api/QueryKeys";
import { AvtaleService } from "@tiltaksadministrasjon/api-client";
import { useApiQuery, useApiSuspenseQuery } from "@mr/frontend-common";

export function useAvtale(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.avtale(id),
    queryFn: async () => {
      return AvtaleService.getAvtale({ path: { id } });
    },
  });
}

export function usePotentialAvtale(id?: string) {
  return useApiQuery({
    queryKey: QueryKeys.avtale(id),
    queryFn: async () => {
      return AvtaleService.getAvtale({ path: { id: id ?? "" } });
    },
    enabled: !!id,
  });
}

export function useAvtaleHandlinger(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.avtaleHandlnger(id),
    queryFn: async () => {
      return AvtaleService.getAvtaleHandlinger({ path: { id } });
    },
  });
}
