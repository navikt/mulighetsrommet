import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ArenaEnkeltplassDto,
  EnkeltplassService,
  ProblemDetail,
  MigrerEnkeltplassResponse,
  MigrerEnkeltplassRequest,
} from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiMutation } from "@/hooks/useApiMutation";
import { useNavigate } from "react-router";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useEnkeltplassHandlinger() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.enkeltplassHandlinger(),
    queryFn: () => EnkeltplassService.getEnkeltplassHandlinger(),
  });
}

export function erArenaEnkeltplassSok(search: string) {
  return /^(\d{4}[#/]\d+|\d{11})$/.test(search.trim());
}

export function useFinnArenaEnkeltplass(search: string, enabled: boolean) {
  return useQuery<ArenaEnkeltplassDto[], ProblemDetail>({
    queryKey: QueryKeys.arenaEnkeltplasser(search),
    enabled: enabled && erArenaEnkeltplassSok(search),
    retry: false,
    gcTime: 0,
    throwOnError: false,
    refetchOnWindowFocus: false,
    queryFn: async ({ signal }) => {
      const { data } = await EnkeltplassService.finnArenaEnkeltplass({
        body: { search },
        signal,
      });
      return data;
    },
  });
}

export function useMigrerEnkeltplass() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  return useApiMutation<
    MigrerEnkeltplassResponse,
    ProblemDetail,
    { id: string; body: MigrerEnkeltplassRequest }
  >({
    mutationFn: async ({ id, body }) => {
      const { data } = await EnkeltplassService.migrerEnkeltplass({ path: { id }, body });
      return data;
    },
    async onSuccess({ id }) {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: QueryKeys.gjennomforinger() }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.gjennomforing(id) }),
        queryClient.invalidateQueries({ queryKey: QueryKeys.arenaEnkeltplasser() }),
      ]);
      navigate(`/gjennomforinger/${id}`);
    },
  });
}
