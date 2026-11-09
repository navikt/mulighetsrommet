import { useKostnadssteder } from "@/api/enhet/useKostnadssteder";
import { useFilterTracking } from "./useFilterTracking";
import type { FilterKilde } from "./useUmami";

export function useKostnadsstedTracking(kontekst?: string) {
  const { data: regioner } = useKostnadssteder();
  const { logFilterEndret } = useFilterTracking(kontekst);

  const logKostnadsstederEndret = (
    forrigeValg: string[],
    nyeValg: string[],
    kilde: FilterKilde,
  ) => {
    if (kontekst === undefined) return;

    const forrigeKostnadssteder = new Set(forrigeValg);
    const nesteKostnadssteder = new Set(nyeValg);
    const endredeKostnadssteder = regioner.flatMap(({ region, kostnadssteder }) =>
      kostnadssteder
        .filter(
          ({ enhetsnummer }) =>
            forrigeKostnadssteder.has(enhetsnummer) !== nesteKostnadssteder.has(enhetsnummer),
        )
        .map((kontor) => ({ regionnavn: region.navn, kontor })),
    );

    for (const { regionnavn, kontor } of endredeKostnadssteder) {
      logFilterEndret({
        kategori: "Kostnadssted",
        filternavn: regionnavn,
        subFilter: kontor.navn,
        handling: nesteKostnadssteder.has(kontor.enhetsnummer) ? "valgt" : "fjernet",
        kilde,
      });
    }
  };

  return { logKostnadsstederEndret };
}
