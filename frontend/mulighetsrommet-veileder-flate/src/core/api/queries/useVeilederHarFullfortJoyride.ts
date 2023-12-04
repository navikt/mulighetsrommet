import { useQuery } from "@tanstack/react-query";
import { JoyrideType } from "mulighetsrommet-api-client";
import { QueryKeys } from "../query-keys";
import { mulighetsrommetClient } from "../clients";

export function useVeilederHarFullfortJoyride(joyrideType: JoyrideType) {
  return useQuery({
    queryKey: QueryKeys.harFullfortJoyride(joyrideType),
    queryFn: async () => {
      return await mulighetsrommetClient.joyride.veilederHarFullfortJoyride({
        joyrideType,
      });
    },
  });
}
