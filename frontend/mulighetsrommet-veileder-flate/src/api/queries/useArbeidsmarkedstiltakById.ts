import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { useGetTiltaksgjennomforingIdFraUrl } from "@/hooks/useGetTiltaksgjennomforingIdFraUrl";
import {
  VeilederflateTiltak,
  VeilederflateTiltakEnkeltplass,
  VeilederflateTiltakEnkeltplassAnskaffet,
  VeilederflateTiltakGruppe,
  VeilederTiltakService,
} from "@mr/api-client";
import { gjennomforingIsAktiv } from "@mr/frontend-common/utils/utils";

export function isTiltakGruppe(tiltak: VeilederflateTiltak): tiltak is VeilederflateTiltakGruppe {
  return tiltak.type === "TILTAK_GRUPPE";
}

export function isTiltakAktivt(gjennomforing: VeilederflateTiltak): boolean {
  if (typeof gjennomforing.status === "string") {
    return gjennomforingIsAktiv(gjennomforing.status);
  } else {
    return gjennomforingIsAktiv(gjennomforing.status.status);
  }
}

export function isTiltakEgenRegi(
  tiltak: VeilederflateTiltak,
): tiltak is VeilederflateTiltakEnkeltplassAnskaffet {
  return tiltak.type === "TILTAK_EGEN_REGI";
}

export function isTiltakEnkeltplass(
  tiltak: VeilederflateTiltak,
): tiltak is VeilederflateTiltakEnkeltplass {
  return tiltak.type === "TILTAK_ENKELTPLASS";
}

export function isTiltakEnkeltplassAnskaffet(
  tiltak: VeilederflateTiltak,
): tiltak is VeilederflateTiltakEnkeltplassAnskaffet {
  return tiltak.type === "TILTAK_ENKELTPLASS_ANSKAFFET";
}

export function isTiltakMedArrangor(
  tiltak: VeilederflateTiltak,
): tiltak is VeilederflateTiltakGruppe | VeilederflateTiltakEnkeltplassAnskaffet {
  return isTiltakGruppe(tiltak) || isTiltakEnkeltplassAnskaffet(tiltak);
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
