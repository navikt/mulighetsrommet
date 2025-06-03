import { VeilederflateTiltak } from "@mr/api-client-v2";

/**
 * Bestemmer tilgang til en lenke som peker veileder til et eget skjema i Porten for tilbakemeldinger på innhold om
 * tiltak.
 * Dette er et prøveprosjekt som foreløpig ikke er rullet ut til alle fylker.
 */
export function isTilbakemeldingerEnabled(tiltak: VeilederflateTiltak): boolean {
  if (tiltak.fylker.length === 0) {
    return false;
  }

  const fylkerMedStotteForTilbakemeldingerViaPorten = [
    "0200", // Øst-Viken
  ];
  return tiltak.fylker.every((fylke) =>
    fylkerMedStotteForTilbakemeldingerViaPorten.includes(fylke),
  );
}

/**
 * Bestemmer om fanen for "oppskrifter" skal vises for tiltaket eller ikke.
 * Oppskriftene blir definert i Sanity og lenket mot en eller flere tiltakstyper.
 */
export function isOppskrifterEnabled(tiltak: VeilederflateTiltak): boolean {
  if (tiltak.fylker.length === 0) {
    return true;
  }

  const fylkerSomIkkeVilHaOppskrifter = [
    "0800", // Vestfold og Telemark
  ];
  return !tiltak.fylker.every((fylke) => fylkerSomIkkeVilHaOppskrifter.includes(fylke));
}
