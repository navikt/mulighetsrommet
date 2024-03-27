import { useMutation, useQuery } from "@tanstack/react-query";
import { JoyrideType } from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";
import { mulighetsrommetClient } from "../client";
import { QueryKeys } from "../query-keys";

export function useJoyride(joyrideType: JoyrideType) {
  const query = useQuery({
    queryKey: QueryKeys.harFullfortJoyride(joyrideType),
    queryFn: async () => {
      return await mulighetsrommetClient.joyride.veilederHarFullfortJoyride({
        joyrideType,
      });
    },
  });
  const { isLoading, data } = query;
  const [isReady, setIsReady] = useState(!isLoading && !data);

  useEffect(() => {
    setIsReady(!isLoading && !data);
  }, [isLoading, data]);

  const mutation = useMutation({
    mutationFn: (data: { joyrideType: JoyrideType; fullfort: boolean }) =>
      mulighetsrommetClient.joyride.lagreJoyrideHarKjort({
        requestBody: { ...data },
      }),
  });

  return {
    isReady,
    setIsReady,
    harFullfort: !!query.data,
    setHarFullfort(fullfort: boolean) {
      setIsReady(!fullfort);
      return mutation.mutate({ joyrideType, fullfort });
    },
  };
}
