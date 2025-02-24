import { AnsattService, TilsagnService } from "@mr/api-client-v2";
import { LoaderFunctionArgs } from "react-router";

import { QueryClient, queryOptions } from "@tanstack/react-query";
import { QueryKeys } from "../../../../api/QueryKeys";
import { gjennomforingQuery } from "../../gjennomforingLoaders";

const ansattQuery = () =>
  queryOptions({
    queryKey: [QueryKeys.ansatt()],
    queryFn: () => AnsattService.hentInfoOmAnsatt(),
  });

const tilsagnQuery = (tilsagnId: string) =>
  queryOptions({
    queryKey: [QueryKeys.getTilsagn(tilsagnId)],
    queryFn: () => TilsagnService.getTilsagn({ path: { id: tilsagnId } }),
  });

const tilsagnHistorikkQuery = (tilsagnId: string) =>
  queryOptions({
    queryKey: ["tilsagn", tilsagnId, "historikk"],
    queryFn: () => TilsagnService.getTilsagnEndringshistorikk({ path: { id: tilsagnId } }),
  });

export const tilsagnDetaljerLoader =
  (queryClient: QueryClient) =>
  async ({ params }: LoaderFunctionArgs) => {
    const { gjennomforingId, tilsagnId } = params;

    if (!gjennomforingId) {
      throw new Error("gjennomforingId is missing");
    }

    if (!tilsagnId) {
      throw new Error("tilsagnId is missing");
    }

    const [{ data: ansatt }, { data: gjennomforing }, { data: tilsagn }, { data: historikk }] =
      await Promise.all([
        queryClient.ensureQueryData(ansattQuery()),
        queryClient.ensureQueryData(gjennomforingQuery(gjennomforingId)),
        queryClient.ensureQueryData(tilsagnQuery(tilsagnId)),
        queryClient.ensureQueryData(tilsagnHistorikkQuery(tilsagnId)),
      ]);

    return { ansatt, gjennomforing, tilsagn, historikk };
  };
