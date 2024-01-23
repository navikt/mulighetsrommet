import { NavEnhet, NavEnhetStatus, NavEnhetType } from "mulighetsrommet-api-client";
import { useNavEnheter } from "./useNavEnheter";

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
  const { data: navEnheter, ...rest } = useNavEnheter(
    [NavEnhetStatus.AKTIV],
    [NavEnhetType.FYLKE, NavEnhetType.LOKAL, NavEnhetType.KO],
  );

  const lokaleEnheterOgEgneAnsatte = navEnheter?.filter(
    (enhet: NavEnhet) =>
      enhet.type === NavEnhetType.LOKAL || EGNE_ANSATTE_NUMMER.includes(enhet.enhetsnummer),
  );
  const regioner = navEnheter?.filter((enhet: NavEnhet) => enhet.type === NavEnhetType.FYLKE);

  const map = new Map<NavEnhet, NavEnhet[]>();
  regioner?.forEach((region: NavEnhet) =>
    map.set(
      region,
      lokaleEnheterOgEgneAnsatte?.filter(
        (enhet: NavEnhet) => enhet.overordnetEnhet === region.enhetsnummer,
      ) ?? [],
    ),
  );

  return { data: map, ...rest };
}
