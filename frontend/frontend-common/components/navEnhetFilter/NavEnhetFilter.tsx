import { ChevronDownIcon } from "@navikt/aksel-icons";
import { Checkbox } from "@navikt/ds-react";
import classnames from "classnames";
import { useState } from "react";
import styles from "./NavEnhetFilter.module.scss";
import { addOrRemove } from "../../utils/utils";

interface NavRegion {
    enhetsnummer: string;
    navn: string;
    enheter: Array<NavEnhet>;
}

interface NavEnhet {
    enhetsnummer: string;
    overordnetEnhet: string | null;
    navn: string;
    type: string;
}

interface RegionMap {
  [region: string]: NavEnhet[];
}

interface Props {
  value: NavEnhet[];
  onChange: (navEnheter: string[]) => void;
  regioner: NavRegion[];
}

export function NavEnhetFilter({ value, onChange, regioner }: Props) {
  const regionMap = buildRegionMap(value);
  const [regionOpen, setRegionOpen] = useState<string[]>([]);

  function regionMapToNavEnheter(regionMap: RegionMap): string[] {
    return Array.from(Object.values(regionMap)).flat(1).map(e => e.enhetsnummer);
  }

  function buildRegionMap(navEnheter: NavEnhet[]): RegionMap {
    const map: RegionMap = {};
    navEnheter.forEach((enhet: NavEnhet) => {
      const regionNavn = enhet.overordnetEnhet ?? "unknown";
      if (regionNavn in map) {
        map[regionNavn].push(enhet);
      } else {
        map[regionNavn] = [enhet];
      }
    });

    return map;
  }

  function underenhetCount(region: NavRegion): number {
    return regionMap[region.enhetsnummer]?.length ?? 0;
  }

  function regionIsIndeterminate(region: NavRegion): boolean {
    const count = underenhetCount(region);
    return count > 0 && count < region.enheter.length;
  }

  function regionIsChecked(region: NavRegion): boolean {
    return underenhetCount(region) === region.enheter.length;
  }

  function regionOnChange(region: NavRegion) {
    const count = underenhetCount(region);

    onChange(
      regionMapToNavEnheter({
        ...regionMap,
        // TODO: satt til sjekk direkte med "LOKAL" for å gjøre det enklere (på kort sikt)
        //       det er mu logikk rundt nav-enheter som trenger en overhaling...
        [region.enhetsnummer]: count > 0 ? [] : region.enheter.filter((enhet) => enhet.type == "LOKAL"),
      }),
    );
  }

  function underenhetIsChecked(enhet: NavEnhet, region: NavRegion): boolean {
    return (regionMap[region.enhetsnummer] ?? []).some(
      (e) => e.enhetsnummer === enhet.enhetsnummer,
    );
  }

  function underenhetOnChange(enhet: NavEnhet) {
    onChange(addOrRemove(value, enhet).map(e => e.enhetsnummer));
  }

  return (
    <>
      {regioner?.map((region: NavRegion) => (
        <div key={region.enhetsnummer}>
          <div
            className={styles.checkbox_and_caret}
            onClick={() => setRegionOpen([...addOrRemove(regionOpen, region.enhetsnummer)])}
          >
            <div onClick={(e) => e.stopPropagation()} className={styles.checkbox}>
              <Checkbox
                size="small"
                key={region.enhetsnummer}
                checked={regionIsChecked(region)}
                onChange={() => regionOnChange(region)}
                indeterminate={regionIsIndeterminate(region)}
              >
                {region.navn}
              </Checkbox>
            </div>
            <div className={styles.caret_container}>
              <ChevronDownIcon
                aria-label="Ikon ned"
                fontSize="1.25rem"
                className={classnames(styles.accordion_down, {
                  [styles.accordion_down_open]: regionOpen.includes(region.enhetsnummer),
                })}
              />
            </div>
          </div>
          {regionOpen.includes(region.enhetsnummer) && (
            <div style={{ marginLeft: "1rem" }}>
              {region.enheter.map((enhet: NavEnhet) => (
                <Checkbox
                  checked={underenhetIsChecked(enhet, region)}
                  onChange={() => underenhetOnChange(enhet)}
                  key={enhet.enhetsnummer}
                  size="small"
                >
                  {enhet.navn}
                </Checkbox>
              ))}
            </div>
          )}
        </div>
      ))}
    </>
  );
}
