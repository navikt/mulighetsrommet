import { UtdanningerService } from "@mr/api-client";
import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";

export function useHentUtdanninger() {
  return useQuery({
    queryKey: QueryKeys.utdanninger(),
    queryFn: () => UtdanningerService.getUtdanninger(),
  });
}
