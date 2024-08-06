import { useQuery } from "@tanstack/react-query";
import { ApiError, TilsagnDto, TilsagnService } from "mulighetsrommet-api-client";
import { QueryKeys } from "../../../api/QueryKeys";
import { useParams } from "react-router-dom";

export function useGetTilsagnById() {
  const { tilsagnId: id } = useParams();

  return useQuery<TilsagnDto, ApiError, TilsagnDto>({
    queryFn: () => TilsagnService.getTilsagn({ id: id! }),
    queryKey: QueryKeys.getTilsagn(id),
    enabled: !!id,
  });
}
