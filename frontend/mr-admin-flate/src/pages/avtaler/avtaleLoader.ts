import { AvtalerService, NavEnheterService, NavEnhetStatus } from "@mr/api-client-v2";
import { QueryClient, queryOptions } from "@tanstack/react-query";
import { LoaderFunctionArgs } from "react-router";
import { ansattQuery } from "../../api/ansatt/ansattQuery";
import { QueryKeys } from "../../api/QueryKeys";
import { tiltakstyperQuery } from "../tiltakstyper/tiltakstypeLoaders";

export const avtaleQuery = (id: string) =>
  queryOptions({
    queryKey: QueryKeys.avtale(id),
    queryFn: async () => {
      const avtale = await AvtalerService.getAvtale({ path: { id } });
      return avtale.data;
    },
  });

export const avtaleLoader =
  (queryClient: QueryClient) =>
  async ({ params }: LoaderFunctionArgs) => {
    if (!params.avtaleId) {
      throw Error("Fant ikke avtaleId i route");
    }

    const [avtale, ansatt] = await Promise.all([
      await queryClient.ensureQueryData(avtaleQuery(params.avtaleId)),
      await queryClient.ensureQueryData(ansattQuery),
    ]);

    return { avtale, ansatt };
  };

const navEnheterQuery = queryOptions({
  queryKey: QueryKeys.navRegioner(),
  queryFn: () =>
    NavEnheterService.getEnheter({
      query: {
        statuser: [
          NavEnhetStatus.AKTIV,
          NavEnhetStatus.UNDER_AVVIKLING,
          NavEnhetStatus.UNDER_ETABLERING,
        ],
      },
    }),
});

export const avtaleSkjemaQuery = (id: string) =>
  queryOptions({
    queryKey: QueryKeys.avtale(id),
    queryFn: async () => {
      const avtale = await AvtalerService.getAvtale({ path: { id } });
      if (!avtale.data) {
        return undefined;
      }
      return avtale.data;
    },
  });

export const avtaleSkjemaLoader =
  (queryClient: QueryClient) =>
  async ({ params }: LoaderFunctionArgs) => {
    const [avtale, tiltakstyper, ansatt, { data: enheter }] = await Promise.all([
      params.avtaleId
        ? await queryClient.ensureQueryData(avtaleSkjemaQuery(params.avtaleId))
        : undefined,
      await queryClient.ensureQueryData(tiltakstyperQuery),
      await queryClient.ensureQueryData(ansattQuery),
      await queryClient.ensureQueryData(navEnheterQuery),
    ]);

    return { avtale, tiltakstyper, ansatt, enheter };
  };
