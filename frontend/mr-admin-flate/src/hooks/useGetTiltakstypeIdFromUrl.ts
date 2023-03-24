import { useParams } from "react-router-dom";

export function useGetTiltakstypeIdFromUrl() {
  const { tiltakstypeId } = useParams<{ tiltakstypeId: string }>();
  return tiltakstypeId;
}
