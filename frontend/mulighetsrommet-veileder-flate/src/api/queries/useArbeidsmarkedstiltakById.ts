import { QueryKeys } from "../query-keys";
import { useTiltakIdFraUrl } from "@/hooks/useTiltakIdFraUrl";
import {
  GjennomforingStatusType,
  VeilederflateTiltak,
  VeilederflateTiltakEgenRegi,
  VeilederflateTiltakEnkeltplass,
  VeilederflateTiltakEnkeltplassAnskaffet,
  VeilederflateTiltakGruppe,
  VeilederTiltakService,
} from "@api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function isTiltakGruppe(tiltak: VeilederflateTiltak): tiltak is VeilederflateTiltakGruppe {
  return (
    tiltak.type === "no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakGruppe"
  );
}

export function isTiltakAktivt(tiltak: VeilederflateTiltak): boolean {
  return !isTiltakGruppe(tiltak) || tiltak.status.type === GjennomforingStatusType.GJENNOMFORES;
}

export function isTiltakEgenRegi(
  tiltak: VeilederflateTiltak,
): tiltak is VeilederflateTiltakEgenRegi {
  return (
    tiltak.type === "no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakEgenRegi"
  );
}

export function isTiltakEnkeltplass(
  tiltak: VeilederflateTiltak,
): tiltak is VeilederflateTiltakEnkeltplass {
  return (
    tiltak.type === "no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakEnkeltplass"
  );
}

export function isTiltakEnkeltplassAnskaffet(
  tiltak: VeilederflateTiltak,
): tiltak is VeilederflateTiltakEnkeltplassAnskaffet {
  return (
    tiltak.type ===
    "no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakEnkeltplassAnskaffet"
  );
}

export function isTiltakMedArrangor(
  tiltak: VeilederflateTiltak,
): tiltak is VeilederflateTiltakGruppe | VeilederflateTiltakEnkeltplassAnskaffet {
  return isTiltakGruppe(tiltak) || isTiltakEnkeltplassAnskaffet(tiltak);
}

export function useModiaArbeidsmarkedstiltakById() {
  const id = useTiltakIdFraUrl();

  return useApiSuspenseQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.tiltakById(id),
    queryFn: () => VeilederTiltakService.getVeilederTiltak({ path: { id } }),
  });
}

export function useNavArbeidsmarkedstiltakById() {
  const id = useTiltakIdFraUrl();

  return useApiSuspenseQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.tiltakById(id),
    queryFn: () => VeilederTiltakService.getNavTiltak({ path: { id } }),
  });
}

export function usePreviewArbeidsmarkedstiltakById() {
  const id = useTiltakIdFraUrl();

  return useApiSuspenseQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.previewTiltakById(id),
    queryFn: () => VeilederTiltakService.getPreviewTiltak({ path: { id } }),
  });
}
