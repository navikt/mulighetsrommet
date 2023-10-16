import { useParams } from "react-router-dom";

export function useGetTiltaksgjennomforingIdFraUrl() {
  const { id } = useParams();
  return normalizeId(id);
}

/**
 * Funksjon for å normalisere id når vi har draft-id'er. Draft-ider er på formatet draft.<id>, men punktum i url for react-router, så vi må bruke underscore istedenfor.
 * Når vi skal bruke draft-iden i Sanity, må den være på normalt format med draft.<id>.
 * @param id
 * @returns
 */
function normalizeId(id?: string): string {
  if (!id) throw new Error("id ikke satt");

  return id?.includes("_") ? id?.replace("_", ".") : id;
}
