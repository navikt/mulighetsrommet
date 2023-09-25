import { useParams } from "react-router-dom";

export function useGetTiltaksgjennomforingIdFraUrl() {
  const { tiltaksnummer = "" } = useParams();
  return normalizeId(tiltaksnummer);
}

/**
 * Funksjon for å normalisere id når vi har draft-id'er. Draft-ider er på formatet draft.<id>, men punktum i url for react-router, så vi må bruke underscore istedenfor.
 * Når vi skal bruke draft-iden i Sanity, må den være på normalt format med draft.<id>.
 * @param tiltaksnummer
 * @returns
 */
function normalizeId(tiltaksnummer?: string): string {
  if (!tiltaksnummer) throw new Error("Tiltaksnummer ikke satt");

  return tiltaksnummer?.includes("_") ? tiltaksnummer?.replace("_", ".") : tiltaksnummer;
}
