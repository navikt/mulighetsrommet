import { ArenaNavEnhet, NavEnhet } from "@mr/api-client";

export function getDisplayName(enhet: NavEnhet | ArenaNavEnhet) {
  const { enhetsnummer, navn } = enhet;
  return navn ? `${enhetsnummer} ${navn}` : enhetsnummer;
}
