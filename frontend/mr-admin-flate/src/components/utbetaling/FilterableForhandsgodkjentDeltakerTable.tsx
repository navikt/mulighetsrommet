import { DeltakerForKostnadsfordeling, NavEnhet, NavRegion } from "@mr/api-client-v2";
import { ForhandsgodkjentDeltakerTable } from "./ForhandsgodkjentDeltakerTable";
import { useMemo, useState } from "react";
import { HGrid } from "@navikt/ds-react";
import { NavEnhetFilter } from "@mr/frontend-common";

interface Props {
  deltakere: DeltakerForKostnadsfordeling[];
  sats: number;
}

export function FilterableForhandsgodkjentDeltakerTable({ deltakere, sats }: Props) {
  const [navEnheter, setNavEnheter] = useState<NavEnhet[]>([]);

  const unikeEnheter = Array.from(
    new Map(
      deltakere
        .map((d) => d.geografiskEnhet)
        .filter((x): x is NonNullable<typeof x> => x != null)
        .map((enhet) => [enhet.enhetsnummer, enhet]),
    ).values(),
  );

  const filteredDeltakere = useMemo(() => {
    if (navEnheter.length === 0) {
      return deltakere;
    } else {
      return deltakere
        .filter((x): x is NonNullable<typeof x> => x != null)
        .filter(
          (d) =>
            d.geografiskEnhet &&
            navEnheter.map((e) => e.enhetsnummer).includes(d.geografiskEnhet.enhetsnummer),
        );
    }
  }, [deltakere, navEnheter]);

  function regioner(): NavRegion[] {
    const map: { [enhetsnummer: string]: NavRegion } = {};
    deltakere
      .map((d) => d.region)
      .filter((x): x is NonNullable<typeof x> => x != null)
      .forEach((region: NavEnhet) => {
        map[region.enhetsnummer] = {
          ...region,
          enheter: [],
        };
      });

    unikeEnheter.forEach((enhet: NavEnhet) => {
      if (!enhet.overordnetEnhet) return;

      map[enhet.overordnetEnhet].enheter.push(enhet);
    });

    return Object.values(map);
  }

  return (
    <HGrid columns="0.2fr 1fr" gap="2" align="start">
      <NavEnhetFilter
        navEnheter={navEnheter}
        setNavEnheter={(enheter: string[]) => {
          setNavEnheter(unikeEnheter.filter((enhet) => enheter.includes(enhet.enhetsnummer)));
        }}
        regioner={regioner()}
      />
      <ForhandsgodkjentDeltakerTable sats={sats} deltakere={filteredDeltakere} />
    </HGrid>
  );
}
