import { ChevronDownIcon } from "@navikt/aksel-icons";
import { Checkbox } from "@navikt/ds-react";
import classnames from "classnames";
import { useState } from "react";
import styles from "./NavEnhetFilter.module.scss";
import { addOrRemove } from "../../utils/utils";
import { NavEnhet, NavRegion } from "mulighetsrommet-api-client";

interface RegionMap {
  [region: string]: NavEnhet[];
}

interface Props {
  navEnheter: NavEnhet[];
  setNavEnheter: (navEnheter: NavEnhet[]) => void;
  regioner: NavRegion[];
}

export function NavEnhetFilter({ navEnheter, setNavEnheter, regioner }: Props) {
  const regionMap = buildRegionMap(navEnheter);
  const [regionOpen, setRegionOpen] = useState<string[]>([]);

  function regionMapToNavEnheter(regionMap: RegionMap): NavEnhet[] {
    return Array.from(Object.values(regionMap)).flat(1);
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

    setNavEnheter(
      regionMapToNavEnheter({
        ...regionMap,
        [region.enhetsnummer]: count > 0 ? [] : region.enheter,
      }),
    );
  }

  function underenhetIsChecked(enhet: NavEnhet, region: NavRegion): boolean {
    return (regionMap[region.enhetsnummer] ?? []).some(
      (e) => e.enhetsnummer === enhet.enhetsnummer,
    );
  }

  function underenhetOnChange(enhet: NavEnhet) {
    setNavEnheter(addOrRemove(navEnheter, enhet));
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
