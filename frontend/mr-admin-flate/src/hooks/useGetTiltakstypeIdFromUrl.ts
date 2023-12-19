import { useParams } from "react-router-dom";

export function useGetTiltakstypeIdFromUrl() {
  const { tiltakstypeId } = useParams<{
    tiltakstypeId: string;
  }>();
  return tiltakstypeId;
}

export function useGetTiltakstypeIdFromUrlOrThrow() {
  const id = useGetTiltakstypeIdFromUrl();

  if (!id) {
    throw Error("TiltakstypeId er ikke satt i URL");
  }

  return id;
}
