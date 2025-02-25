import {
  AvtaleDto,
  AvtalerService,
  NavAnsatt,
  NavEnhet,
  NavEnheterService,
  NavEnhetStatus,
  PaginertTiltakstype,
} from "@mr/api-client-v2";
import { QueryClient, queryOptions } from "@tanstack/react-query";
import { LoaderFunctionArgs } from "react-router";
import { ansattQuery } from "../../api/ansatt/ansattQuery";
import { QueryKeys } from "../../api/QueryKeys";
import { tiltakstyperQuery } from "../tiltakstyper/tiltakstypeLoaders";

export const avtaleQuery = (id: string) =>
  queryOptions({
    queryKey: [QueryKeys.avtale(id)],
    queryFn: async () => await AvtalerService.getAvtale({ path: { id } }),
  });

export const avtaleLoader =
  (queryClient: QueryClient) =>
  async ({ params }: LoaderFunctionArgs) => {
    if (!params.avtaleId) {
      throw Error("Fant ikke avtaleId i route");
    }

    const [{ data: avtale }, { data: ansatt }] = await Promise.all([
      queryClient.ensureQueryData(avtaleQuery(params.avtaleId)),
      queryClient.ensureQueryData(ansattQuery),
    ]);
    return { avtale, ansatt };
  };

const navEnheterQuery = queryOptions({
  queryKey: [QueryKeys.navRegioner()],
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

export const avtaleSkjemaLoader =
  (queryClient: QueryClient) =>
  async ({ params }: LoaderFunctionArgs) => {
    const [{ data: avtale }, tiltakstyper, { data: ansatt }, { data: enheter }] = await Promise.all(
      [
        params.avtaleId
          ? await queryClient.ensureQueryData(avtaleQuery(params.avtaleId))
          : { data: undefined },
        await queryClient.ensureQueryData(tiltakstyperQuery),
        await queryClient.ensureQueryData(ansattQuery),
        await queryClient.ensureQueryData(navEnheterQuery),
      ],
    );

    return { avtale, tiltakstyper, ansatt, enheter } as {
      avtale: AvtaleDto;
      tiltakstyper: PaginertTiltakstype;
      ansatt: NavAnsatt;
      enheter: NavEnhet[];
    };
  };
