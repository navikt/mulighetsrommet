import { Prismodell, TilsagnService, TilsagnStatus, TilsagnType } from "@mr/api-client-v2";
import { LoaderFunctionArgs } from "react-router";

import { QueryClient, queryOptions } from "@tanstack/react-query";
import { QueryKeys } from "../../../../api/QueryKeys";
import { avtaleQuery } from "../../../avtaler/avtaleLoader";
import { gjennomforingQuery } from "../../gjennomforingLoaders";

const tilsagnDefaultsQuery = (params: {
  gjennomforingId: string;
  type: TilsagnType;
  prismodell: Prismodell | null;
  periodeStart: string | null;
  periodeSlutt: string | null;
  belop: number | null;
  kostnadssted: string | null;
}) =>
  queryOptions({
    queryKey: [QueryKeys.opprettTilsagn(), params],
    queryFn: () =>
      TilsagnService.getTilsagnDefaults({
        body: params,
      }),
  });

const godkjenteTilsagnQuery = (gjennomforingId: string) =>
  queryOptions({
    queryKey: [QueryKeys.getTilsagnForGjennomforing(gjennomforingId)],
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

export const opprettTilsagnLoader =
  (queryClient: QueryClient) =>
  async ({ params, request }: LoaderFunctionArgs) => {
    const { gjennomforingId } = params;

    if (!gjennomforingId) {
      throw new Error("gjennomforingId is missing");
    }

    const url = new URL(request.url);
    const type = (url.searchParams.get("type") as TilsagnType) ?? TilsagnType.TILSAGN;
    const periodeStart = url.searchParams.get("periodeStart");
    const periodeSlutt = url.searchParams.get("periodeSlutt");
    const belop = url.searchParams.get("belop");
    const prismodell = url.searchParams.get("prismodell")
      ? (url.searchParams.get("prismodell") as Prismodell)
      : null;
    const kostnadssted = url.searchParams.get("kostnadssted");

    const [{ data: gjennomforing }, { data: defaults }, { data: godkjenteTilsagn }] =
      await Promise.all([
        queryClient.ensureQueryData(gjennomforingQuery(gjennomforingId)),
        queryClient.ensureQueryData(
          tilsagnDefaultsQuery({
            gjennomforingId,
            type,
            prismodell,
            periodeStart,
            periodeSlutt,
            belop: belop ? Number(belop) : null,
            kostnadssted,
          }),
        ),
        queryClient.ensureQueryData(godkjenteTilsagnQuery(gjennomforingId)),
      ]);

    const { data: avtale } = await queryClient.ensureQueryData(
      avtaleQuery(gjennomforing.avtaleId!),
    );

    return { avtale, gjennomforing, defaults, godkjenteTilsagn };
  };
