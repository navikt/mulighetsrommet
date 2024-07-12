import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ApiError,
  DialogenService,
  DialogRequest,
  DialogResponse,
} from "mulighetsrommet-api-client";
import { QueryKeys } from "../query-keys";

interface Props {
  onSuccess?: (response: DialogResponse) => void;
}

export function useDelTiltakViaDialogen({ onSuccess }: Props) {
  const queryClient = useQueryClient();
  return useMutation<DialogResponse, ApiError, DialogRequest>({
    mutationKey: QueryKeys.DeltMedBrukerStatus,
    mutationFn: async (requestBody) => DialogenService.delMedDialogen({ requestBody }),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: QueryKeys.DeltMedBrukerStatus });
      onSuccess?.(response);
    },
  });
}
