import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingDetaljerDto, GjennomforingService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { useRequiredParams } from "@/hooks/useRequiredParams";

export function useEnkeltplassGjennomforingOrError(id: string) {
  const data = useGjennomforing(id);

  if (data.gjennomforing.type !== "GjennomforingEnkeltplassDto") {
    throw "Gjennomføring var ikke enkeltplass";
  }
  return data;
}

export function useGjennomforing(id: string) {
  const result = useApiSuspenseQuery({
    queryKey: QueryKeys.gjennomforing(id),
    queryFn: () => GjennomforingService.getGjennomforing({ path: { id } }),
  });
  return result.data;
}

export function useGjennomforingByPathParam(): GjennomforingDetaljerDto {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  return useGjennomforing(gjennomforingId);
}

export function useGjennomforingHandlinger(id: string) {
  const result = useApiSuspenseQuery({
    queryKey: QueryKeys.gjennomforingHandlinger(id),
    queryFn: () => GjennomforingService.getGjennomforingHandlinger({ path: { id } }),
  });
  return result.data;
}
