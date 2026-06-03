import { PrismodellDto } from "@tiltaksadministrasjon/api-client";

export function useFindAvtaltSats(prismodell: PrismodellDto, periodeStart?: string | null) {
  const satser = prismodell.satser ?? [];
  return satser.findLast((sats) => periodeStart && periodeStart >= sats.gjelderFra);
}
