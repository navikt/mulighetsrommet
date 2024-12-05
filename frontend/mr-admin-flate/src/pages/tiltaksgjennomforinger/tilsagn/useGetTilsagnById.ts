import { useQuery } from "@tanstack/react-query";
import { ApiError, TilsagnDto, TilsagnService } from "@mr/api-client";
import { QueryKeys } from "../../../api/QueryKeys";
import { useParams } from "react-router-dom";

export function useGetTilsagnById() {
  const { tilsagnId: id } = useParams();

  return useQuery<TilsagnDto, ApiError, TilsagnDto>({
    queryKey: QueryKeys.getTilsagn(id!),
    queryFn: () => TilsagnService.getTilsagn({ id: id! }),
    enabled: !!id,
  });
}
