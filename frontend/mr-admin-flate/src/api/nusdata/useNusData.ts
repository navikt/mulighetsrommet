import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { NusDataResponse, Tiltakskode } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../client";

export function useNusData(tiltakskode: Tiltakskode, version: string) {
  return useQuery<NusDataResponse>({
    queryKey: QueryKeys.nusData(tiltakskode, version),
    queryFn: () =>
      mulighetsrommetClient.nus.getNusDataByTiltakstypeAndVersion({
        requestBody: {
          tiltakskode,
          version,
        },
      }),
  });
}
