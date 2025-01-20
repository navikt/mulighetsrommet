import { useParams } from "react-router";

export function useGetGjennomforingIdFromUrl(): string | undefined {
  const { gjennomforingId } = useParams<{
    gjennomforingId: string;
  }>();

  return gjennomforingId;
}

export function useGetGjennomforingIdFromUrlOrThrow(): string {
  const id = useGetGjennomforingIdFromUrl();

  if (!id) {
    throw Error("GjennomføringId er ikke satt i URL");
  }

  return id;
}
