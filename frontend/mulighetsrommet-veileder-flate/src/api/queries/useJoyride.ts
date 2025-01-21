import { useMutation } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { QueryKeys } from "../query-keys";
import { JoyrideService, JoyrideType } from "@mr/api-client-v2";
import { useApiQuery } from "@mr/frontend-common";

export function useJoyride(joyrideType: JoyrideType) {
  const query = useApiQuery({
    queryKey: QueryKeys.harFullfortJoyride(joyrideType),
    queryFn: () => {
      return JoyrideService.veilederHarFullfortJoyride({
        path: { joyrideType },
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
        body: { ...data },
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
