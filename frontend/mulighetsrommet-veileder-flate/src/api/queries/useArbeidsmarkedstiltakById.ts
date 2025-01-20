import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { useTiltakIdFraUrl } from "@/hooks/useTiltakIdFraUrl";
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
  return gjennomforingIsAktiv(gjennomforing.status);
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
  const id = useTiltakIdFraUrl();

  return useSuspenseQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.tiltakById(id),
    queryFn: () => VeilederTiltakService.getVeilederTiltak({ id }),
  });
}

export function useNavArbeidsmarkedstiltakById() {
  const id = useTiltakIdFraUrl();

  return useSuspenseQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.tiltakById(id),
    queryFn: () => VeilederTiltakService.getNavTiltak({ id }),
  });
}

export function usePreviewArbeidsmarkedstiltakById() {
  const id = useTiltakIdFraUrl();

  return useSuspenseQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.previewTiltakById(id),
    queryFn: () => VeilederTiltakService.getPreviewTiltak({ id }),
  });
}
