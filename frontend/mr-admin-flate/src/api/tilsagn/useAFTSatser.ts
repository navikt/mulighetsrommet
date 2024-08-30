import { useQuery } from "@tanstack/react-query";
import { AftSatserService } from "@mr/api-client";
import { QueryKeys } from "../QueryKeys";

export function useAFTSatser() {
  return useQuery({
    queryFn: () => AftSatserService.aftSatser(),
    queryKey: QueryKeys.aftSatser(),
  });
}
