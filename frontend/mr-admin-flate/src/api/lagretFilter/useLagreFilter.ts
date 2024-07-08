import { useMutation } from "@tanstack/react-query";
import { ApiError, LagretFilterRequest } from "mulighetsrommet-api-client";
import { LagretFilterService } from "mulighetsrommet-api-client";

interface Props {
  onSuccess: () => void;
}

export function useLagreFilter({ onSuccess }: Props) {
  return useMutation<any, ApiError, LagretFilterRequest>({
    mutationFn: (requestBody) => LagretFilterService.upsertFilter({ requestBody }),
    onSuccess,
  });
}
