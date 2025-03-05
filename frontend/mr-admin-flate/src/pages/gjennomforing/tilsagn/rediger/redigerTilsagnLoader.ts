import { TilsagnService, TilsagnStatus } from "@mr/api-client-v2";
import { QueryClient, queryOptions } from "@tanstack/react-query";
import { LoaderFunctionArgs } from "react-router";
import { QueryKeys } from "../../../../api/QueryKeys";
import { avtaleQuery } from "../../../avtaler/avtaleLoader";
import { gjennomforingQuery } from "../../gjennomforingLoaders";

const tilsagnQuery = (tilsagnId: string) =>
  queryOptions({
    queryKey: QueryKeys.getTilsagn(tilsagnId),
    queryFn: () => TilsagnService.getTilsagn({ path: { id: tilsagnId } }),
  });

const godkjenteTilsagnQuery = (gjennomforingId: string) =>
  queryOptions({
    queryKey: QueryKeys.getTilsagnForGjennomforing(gjennomforingId),
    queryFn: () =>
      TilsagnService.getAll({
        query: {
          gjennomforingId,
          statuser: [
            TilsagnStatus.GODKJENT,
            TilsagnStatus.TIL_GODKJENNING,
            TilsagnStatus.RETURNERT,
          ],
        },
      }),
  });

export const redigerTilsagnLoader =
  (queryClient: QueryClient) =>
  async ({ params }: LoaderFunctionArgs) => {
    const { gjennomforingId, tilsagnId } = params;

    if (!gjennomforingId) {
      throw new Error("gjennomforingId is missing");
    }

    if (!tilsagnId) {
      throw new Error("tilsagnId is missing");
    }

    const [gjennomforing, { data: tilsagn }, { data: godkjenteTilsagn }] = await Promise.all([
      queryClient.ensureQueryData(gjennomforingQuery(gjennomforingId)),
      queryClient.ensureQueryData(tilsagnQuery(tilsagnId)),
      queryClient.ensureQueryData(godkjenteTilsagnQuery(gjennomforingId)),
    ]);

    const avtale = await queryClient.ensureQueryData(avtaleQuery(gjennomforing.avtaleId!));

    return { avtale, gjennomforing, tilsagn, godkjenteTilsagn };
  };
