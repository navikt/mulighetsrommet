import { NavEnhet, NavEnhetStatus, NavEnhetType } from "mulighetsrommet-api-client";
import { useNavEnheter } from "./useNavEnheter";
import { useState } from "react";

const EGNE_ANSATTE_NUMMER = [
  "1083",
  "0483",
  "1583",
  "1883",
  "0383",
  "1183",
  "1983",
  "1683",
  "0683",
  "0883",
  "1283",
  "0283",
];

export function useRegionMap() {
  const [map, setMap] = useState<Map<NavEnhet, NavEnhet[]>>(new Map());
  const { data: navEnheter, ...rest } = useNavEnheter(
    [NavEnhetStatus.AKTIV],
    [NavEnhetType.FYLKE, NavEnhetType.LOKAL, NavEnhetType.KO],
  );
  const regioner = navEnheter?.filter((enhet: NavEnhet) => enhet.type === NavEnhetType.FYLKE);

  if (navEnheter && map?.size !== regioner?.length) {
    const lokaleEnheterOgEgneAnsatte = navEnheter?.filter(
      (enhet: NavEnhet) =>
        enhet.type === NavEnhetType.LOKAL || EGNE_ANSATTE_NUMMER.includes(enhet.enhetsnummer),
    );

    const newMap: Map<NavEnhet, NavEnhet[]> = new Map();
    regioner?.forEach((region: NavEnhet) => {
      newMap.set(
        region,
        lokaleEnheterOgEgneAnsatte?.filter(
          (enhet: NavEnhet) => enhet.overordnetEnhet === region.enhetsnummer,
        ) ?? [],
      );
    });

    setMap(newMap);
  }

  return { data: map, ...rest };
}
