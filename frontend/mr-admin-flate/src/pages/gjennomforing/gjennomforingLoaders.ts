import { AvtalerService, GjennomforingerService } from "@mr/api-client-v2";
import { QueryClient, queryOptions } from "@tanstack/react-query";
import { LoaderFunctionArgs } from "react-router";
import { ansattQuery } from "../../api/ansatt/ansattQuery";
import { QueryKeys } from "../../api/QueryKeys";
import { avtaleQuery } from "../avtaler/avtaleLoader";

export const gjennomforingLoader =
  (queryClient: QueryClient) =>
  async ({ params }: LoaderFunctionArgs) => {
    if (!params.gjennomforingId) {
      throw Error("Fant ikke gjennomforingId i route");
    }

    const [ansatt, gjennomforing] = await Promise.all([
      queryClient.ensureQueryData(ansattQuery),
      queryClient.ensureQueryData(gjennomforingQuery(params.gjennomforingId)),
    ]);

    const avtaleId = params?.avtaleId || gjennomforing?.avtaleId;
    const { data: avtale } = avtaleId
      ? await AvtalerService.getAvtale({ path: { id: avtaleId } })
      : { data: undefined };

    return { gjennomforing, avtale, ansatt };
  };

export const gjennomforingQuery = (id?: string) =>
  queryOptions({
    queryKey: QueryKeys.gjennomforing(id),
    queryFn: async () =>
      (await GjennomforingerService.getGjennomforing({ path: { id: id! } })).data,
    enabled: !!id,
  });

export const gjennomforingFormLoader =
  (queryClient: QueryClient) =>
  async ({ params }: LoaderFunctionArgs) => {
    const [ansatt, gjennomforing] = await Promise.all([
      await queryClient.ensureQueryData(ansattQuery),
      params.gjennomforingId
        ? await queryClient.ensureQueryData(gjennomforingQuery(params.gjennomforingId))
        : undefined,
    ]);

    const avtaleId = params?.avtaleId || gjennomforing?.avtaleId;
    const avtale = avtaleId ? await queryClient.ensureQueryData(avtaleQuery(avtaleId)) : undefined;

    return { gjennomforing, avtale, ansatt };
  };
