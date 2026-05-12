import {
  TiltakstypeEgenskap,
  TiltakstypeFeature,
  VeilederflateTiltak,
  VeilederflateTiltakstype,
} from "@api-client";

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

export function kanOppretteEnkeltplass(tiltakstype: VeilederflateTiltakstype) {
  return (
    harFeature(tiltakstype, TiltakstypeFeature.MIGRERT) &&
    harEgenskap(tiltakstype, TiltakstypeEgenskap.STOTTER_ENKELTPLASSER)
  );
}

function harFeature(tiltakstype: VeilederflateTiltakstype, feature: TiltakstypeFeature): boolean {
  return tiltakstype.features.includes(feature);
}

function harEgenskap(
  tiltakstype: VeilederflateTiltakstype,
  egenskap: TiltakstypeEgenskap,
): boolean {
  return tiltakstype.egenskaper.includes(egenskap);
}
