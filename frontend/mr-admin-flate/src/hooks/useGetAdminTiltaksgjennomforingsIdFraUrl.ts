import { useParams } from "react-router-dom";

export function useGetAdminTiltaksgjennomforingsIdFraUrl() {
  const { tiltaksgjennomforingId, utkastId } = useParams<{
    tiltaksgjennomforingId: string;
    utkastId: string;
  }>();
  return tiltaksgjennomforingId || utkastId;
}
