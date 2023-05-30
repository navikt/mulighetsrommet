export function isTiltakMedFellesOppstart(tiltakskode: string): boolean {
  const tiltakMedFellesOppstart = ["GRUPPEAMO", "JOBBK", "GRUFAGYRKE"];
  return tiltakMedFellesOppstart.includes(tiltakskode);
}

export const arenaKodeErAftEllerVta = (arenaKode: string = "") =>
  ["ARBFORB", "VASV"].includes(arenaKode);
