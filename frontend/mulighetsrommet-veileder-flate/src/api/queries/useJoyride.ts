import { useMutation, useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { QueryKeys } from "../query-keys";
import { JoyrideService, JoyrideType } from "mulighetsrommet-api-client";

export function useJoyride(joyrideType: JoyrideType) {
  const query = useQuery({
    queryKey: QueryKeys.harFullfortJoyride(joyrideType),
    queryFn: () => {
      return JoyrideService.veilederHarFullfortJoyride({
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
      JoyrideService.lagreJoyrideHarKjort({
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
