import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { useGetTiltaksgjennomforingIdFraUrl } from "@/hooks/useGetTiltaksgjennomforingIdFraUrl";
import { VeilederTiltakService } from "@mr/api-client";

export function useTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();

  return useQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.tiltaksgjennomforing(id),
    queryFn: () => VeilederTiltakService.getVeilederTiltaksgjennomforing({ id }),
  });
}

export function useNavTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();

  return useQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.tiltaksgjennomforing(id),
    queryFn: () => VeilederTiltakService.getNavTiltaksgjennomforing({ id }),
  });
}

export function usePreviewTiltaksgjennomforingById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();

  return useQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.tiltaksgjennomforingPreview(id),
    queryFn: () => VeilederTiltakService.getPreviewTiltaksgjennomforing({ id }),
  });
}
