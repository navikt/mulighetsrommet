import { QueryKeys } from "@/api/QueryKeys";
import { GjennomforingDetaljerDto, GjennomforingService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { isEnkeltplass } from "@/api/gjennomforing/utils";

type EnkeltplassGjennomforing = Extract<
  GjennomforingDetaljerDto["gjennomforing"],
  { type: "GjennomforingEnkeltplassDto" }
>;

type EnkeltplassGjennomforingDetaljer = Omit<GjennomforingDetaljerDto, "gjennomforing"> & {
  gjennomforing: EnkeltplassGjennomforing;
};

export function useEnkeltplassGjennomforingOrError(id: string): EnkeltplassGjennomforingDetaljer {
  const data = useGjennomforing(id);
  const { gjennomforing } = data;

  if (!isEnkeltplass(gjennomforing)) {
    throw `Gjennomføring med id=${id} er ikke en enkeltplass`;
  }

  return { ...data, gjennomforing };
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
