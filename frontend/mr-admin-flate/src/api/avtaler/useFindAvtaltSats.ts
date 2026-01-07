import { GjennomforingDto } from "@tiltaksadministrasjon/api-client";

export function useFindAvtaltSats(gjennomforing: GjennomforingDto, periodeStart?: string | null) {
  const satser = gjennomforing.prismodell?.satser ?? [];
  return satser.findLast((sats) => periodeStart && periodeStart >= sats.gjelderFra);
}
