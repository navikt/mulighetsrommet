import { useApiSuspenseQuery } from "@mr/frontend-common";
import {
  LagretFilter,
  LagretFilterService,
  LagretFilterType,
} from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useQueryClient } from "@tanstack/react-query";
import { useApiMutation } from "@/hooks/useApiMutation";

interface NamedFilter {
  id: string;
  navn: string;
  filter: { [key: string]: unknown };
}

export function useLagredeFilter(dokumenttype: LagretFilterType) {
  const queryClient = useQueryClient();

  const lagreFilterMutation = useApiMutation({
    mutationFn: (body: LagretFilter) => LagretFilterService.upsertFilter({ body }),
    onSuccess: () => {
      return queryClient.invalidateQueries({
        queryKey: QueryKeys.lagredeFilter(dokumenttype),
      });
    },
  });

  const slettFilterMutation = useApiMutation({
    mutationFn: async (id: string) => LagretFilterService.slettLagretFilter({ path: { id } }),
    onSuccess: () => {
      return queryClient.invalidateQueries({
        queryKey: QueryKeys.lagredeFilter(dokumenttype),
      });
    },
  });

  const { data: filters } = useApiSuspenseQuery({
    queryKey: QueryKeys.lagredeFilter(dokumenttype),
    queryFn: () => LagretFilterService.getMineFilterForDokumenttype({ path: { dokumenttype } }),
  });

  function deleteFilter(id: string) {
    slettFilterMutation.mutate(id);
  }

  function saveFilter(filter: NamedFilter) {
    lagreFilterMutation.mutate(
      {
        ...filter,
        type: dokumenttype,
        isDefault: false,
        sortOrder: 0,
      },
      {
        onSuccess() {
          lagreFilterMutation.reset();
        },
      },
    );
  }

  function setDefaultFilter(id: string, isDefault: boolean) {
    const filter = filters.find((f) => f.id === id);
    if (filter) {
      lagreFilterMutation.mutate({ ...filter, isDefault });
    }
  }

  return {
    filters,
    deleteFilter,
    saveFilter,
    setDefaultFilter,
  };
}
