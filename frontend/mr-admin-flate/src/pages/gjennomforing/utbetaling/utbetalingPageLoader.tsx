import { UtbetalingService } from "@mr/api-client-v2";
import { QueryClient, queryOptions } from "@tanstack/react-query";
import { LoaderFunctionArgs } from "react-router";
import { ansattQuery } from "../../../api/ansatt/ansattQuery";
import { gjennomforingQuery } from "../gjennomforingLoaders";

const utbetalingQuery = (utbetalingId: string) =>
  queryOptions({
    queryKey: ["utbetaling", utbetalingId],
    queryFn: () => UtbetalingService.getUtbetaling({ path: { id: utbetalingId } }),
  });

const tilsagnTilUtbetalingQuery = (utbetalingId: string) =>
  queryOptions({
    queryKey: ["utbetaling", utbetalingId, "tilsagn"],
    queryFn: () => UtbetalingService.getTilsagnTilUtbetaling({ path: { id: utbetalingId } }),
  });

const utbetalingHistorikkQuery = (utbetalingId: string) =>
  queryOptions({
    queryKey: ["utbetaling", utbetalingId, "historikk"],
    queryFn: () => UtbetalingService.getUtbetalingEndringshistorikk({ path: { id: utbetalingId } }),
  });

export const utbetalingPageLoader =
  (queryClient: QueryClient) =>
  async ({ params }: LoaderFunctionArgs) => {
    const { gjennomforingId, utbetalingId } = params;

    if (!gjennomforingId) {
      throw new Error("gjennomforingId is missing");
    }
    if (!utbetalingId) {
      throw new Error("utbetalingId is missing");
    }

    const [
      { data: ansatt },
      { data: gjennomforing },
      { data: utbetaling },
      { data: tilsagn },
      { data: historikk },
    ] = await Promise.all([
      queryClient.ensureQueryData(ansattQuery),
      queryClient.ensureQueryData(gjennomforingQuery(gjennomforingId)),
      queryClient.ensureQueryData(utbetalingQuery(utbetalingId)),
      queryClient.ensureQueryData(tilsagnTilUtbetalingQuery(utbetalingId)),
      queryClient.ensureQueryData(utbetalingHistorikkQuery(utbetalingId)),
    ]);

    return { ansatt, gjennomforing, utbetaling, tilsagn, historikk };
  };
