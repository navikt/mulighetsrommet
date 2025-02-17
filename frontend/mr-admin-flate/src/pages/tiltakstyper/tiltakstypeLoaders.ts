import { PaginertTiltakstype, TiltakstyperService } from "@mr/api-client-v2";
import { QueryClient } from "@tanstack/react-query";
import { LoaderFunction, LoaderFunctionArgs } from "react-router";

export const tiltakstyperQuery = {
  queryKey: ["tiltakstyper"],
  queryFn: async () => {
    const { data } = await TiltakstyperService.getTiltakstyper();
    return data;
  },
};

export const tiltakstyperLoader =
  (queryClient: QueryClient): LoaderFunction<Promise<PaginertTiltakstype>> =>
  async () => {
    await queryClient.ensureQueryData(tiltakstyperQuery);
  };

export const tiltakstypeQuery = (id: string) => {
  return {
    queryKey: ["tiltakstype", id],
    queryFn: async () => {
      const { data } = await TiltakstyperService.getTiltakstypeById({ path: { id } });
      return data;
    },
  };
};

export const tiltakstypeLoader =
  (queryClient: QueryClient) =>
  async ({ params }: LoaderFunctionArgs) => {
    if (!params.tiltakstypeId) throw Error("Fant ikke tiltakstypeId i route");

    const tiltakstype = await queryClient.ensureQueryData(tiltakstypeQuery(params.tiltakstypeId));
    return { tiltakstypeId: params.tiltakstypeId, tiltakstype };
  };
