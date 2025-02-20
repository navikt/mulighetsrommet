import { NavEnheterService, TiltakstyperService } from "@mr/api-client-v2";
import { QueryClient } from "@tanstack/react-query";

const tiltakstyperQuery = {
  queryKey: ["tiltakstyper"],
  queryFn: async () => {
    const { data } = await TiltakstyperService.getTiltakstyper();
    return data.data;
  },
};

const regionerQuery = {
  queryKey: ["regioner"],
  queryFn: async () => {
    const { data } = await NavEnheterService.getRegioner();
    return data;
  },
};

export const oppgaverLoader = (queryClient: QueryClient) => async () => {
  const tiltakstyper = await queryClient.ensureQueryData(tiltakstyperQuery);
  const regioner = await queryClient.ensureQueryData(regionerQuery);

  return {
    tiltakstyper,
    regioner,
  };
};
