import { useParams } from "react-router-dom";

export function useGetAdminTiltaksgjennomforingsIdFraUrl() {
  const { tiltaksgjennomforingId } = useParams<{
    tiltaksgjennomforingId: string;
  }>();
  return tiltaksgjennomforingId;
}
