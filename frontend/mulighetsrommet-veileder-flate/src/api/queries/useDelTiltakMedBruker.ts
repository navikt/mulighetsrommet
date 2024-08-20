import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ApiError,
  DelMedBrukerService,
  DelTiltakMedBrukerRequest,
  DelTiltakMedBrukerResponse,
} from "@mr/api-client";
import { QueryKeys } from "../query-keys";

interface Props {
  onSuccess: (response: DelTiltakMedBrukerResponse) => void;
}

export function useDelTiltakMedBruker({ onSuccess }: Props) {
  const queryClient = useQueryClient();
  return useMutation<DelTiltakMedBrukerResponse, ApiError, DelTiltakMedBrukerRequest>({
    mutationKey: QueryKeys.DeltMedBrukerStatus,
    mutationFn: async (requestBody) => DelMedBrukerService.delTiltakMedBruker({ requestBody }),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: QueryKeys.DeltMedBrukerStatus });
      onSuccess(response);
    },
  });
}
