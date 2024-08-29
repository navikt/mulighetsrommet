import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { useGetTiltaksgjennomforingIdFraUrl } from "@/hooks/useGetTiltaksgjennomforingIdFraUrl";
import {
  VeilederflateTiltak,
  VeilederflateTiltakArbeidsgiver,
  VeilederflateTiltakGruppe,
  VeilederTiltakService,
} from "@mr/api-client";

export function isTiltakGruppe(tiltak: VeilederflateTiltak): tiltak is VeilederflateTiltakGruppe {
  return tiltak.type === "TILTAK_GRUPPE";
}

// TODO: legge til et skille på arbeidsgiver-tiltak og IPS/AMS og andre individuelle tiltak som også blir inkludert her
export function isTiltakArbeidsgiver(
  tiltak: VeilederflateTiltak,
): tiltak is VeilederflateTiltakArbeidsgiver {
  return tiltak.type === "TILTAK_ARBEIDSGIVER";
}

export function useModiaArbeidsmarkedstiltakById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();

  return useSuspenseQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.tiltakById(id),
    queryFn: () => VeilederTiltakService.getVeilederTiltaksgjennomforing({ id }),
  });
}

export function useNavArbeidsmarkedstiltakById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();

  return useSuspenseQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.tiltakById(id),
    queryFn: () => VeilederTiltakService.getNavTiltaksgjennomforing({ id }),
  });
}

export function usePreviewArbeidsmarkedstiltakById() {
  const id = useGetTiltaksgjennomforingIdFraUrl();

  return useSuspenseQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.previewTiltakById(id),
    queryFn: () => VeilederTiltakService.getPreviewTiltaksgjennomforing({ id }),
  });
}
