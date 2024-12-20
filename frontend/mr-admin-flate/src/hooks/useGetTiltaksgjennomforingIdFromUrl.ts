import { useParams } from "react-router";

export function useGetTiltaksgjennomforingIdFromUrl(): string | undefined {
  const { tiltaksgjennomforingId } = useParams<{
    tiltaksgjennomforingId: string;
  }>();

  return tiltaksgjennomforingId;
}

export function useGetTiltaksgjennomforingIdFromUrlOrThrow(): string {
  const id = useGetTiltaksgjennomforingIdFromUrl();

  if (!id) {
    throw Error("TiltaksgjennomføringId er ikke satt i URL");
  }

  return id;
}
