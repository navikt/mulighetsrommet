import { UtbetalingService } from "@mr/api-client-v2";
import { QueryClient, queryOptions } from "@tanstack/react-query";
import { LoaderFunctionArgs } from "react-router";
import { QueryKeys } from "../../../api/QueryKeys";
import { gjennomforingQuery } from "../gjennomforingLoaders";

const utbetalingerByGjennomforingQuery = (gjennomforingId: string) =>
  queryOptions({
    queryKey: QueryKeys.utbetalingerByGjennomforing(gjennomforingId),
    queryFn: () => UtbetalingService.utbetalingerByGjennomforing({ path: { gjennomforingId } }),
  });

export const utbetalingerForGjennomforingLoader =
  (queryClient: QueryClient) =>
  async ({ params }: LoaderFunctionArgs) => {
    const { gjennomforingId } = params;

    if (!gjennomforingId) {
      throw Error("Fant ikke gjennomforingId i route");
    }

    const [gjennomforing, { data: utbetalinger }] = await Promise.all([
      queryClient.ensureQueryData(gjennomforingQuery(gjennomforingId)),
      queryClient.ensureQueryData(utbetalingerByGjennomforingQuery(gjennomforingId)),
    ]);

    return { utbetalinger, gjennomforing };
  };
