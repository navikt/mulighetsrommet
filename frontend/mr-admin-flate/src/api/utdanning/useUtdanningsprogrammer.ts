import { UtdanningerService } from "@mr/api-client";
import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";

export function useUtdanningsprogrammer() {
  return useSuspenseQuery({
    queryKey: QueryKeys.utdanninger(),
    queryFn: UtdanningerService.getUtdanningsprogrammer,
  });
}
