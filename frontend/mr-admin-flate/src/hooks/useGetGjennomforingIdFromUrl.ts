import { useParams } from "react-router";

export function useGetGjennomforingIdFromUrl(): string | undefined {
  const { gjennomforingId } = useParams<{
    gjennomforingId: string;
  }>();

  return gjennomforingId;
}
