import { useParams } from "react-router";

export function useGetGjennomforingIdFromUrl(): string | undefined {
  const { tiltaksgjennomforingId } = useParams<{
    tiltaksgjennomforingId: string;
  }>();

  return tiltaksgjennomforingId;
}

export function useGetGjennomforingIdFromUrlOrThrow(): string {
  const id = useGetGjennomforingIdFromUrl();

  if (!id) {
    throw Error("GjennomføringId er ikke satt i URL");
  }

  return id;
}
