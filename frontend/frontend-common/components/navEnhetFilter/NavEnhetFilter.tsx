import { ChevronDownIcon } from "@navikt/aksel-icons";
import { Checkbox } from "@navikt/ds-react";
import classnames from "classnames";
import { useState } from "react";
import styles from "./NavEnhetFilter.module.scss";
import { addOrRemoveBy, addOrRemove } from "../../utils/utils";

interface NavRegion {
  enhetsnummer: string;
  navn: string;
  enheter: Array<NavRegionUnderenhet>;
}

interface NavRegionUnderenhet {
  navn: string;
  enhetsnummer: string;
  overordnetEnhet: string;
  erStandardvalg: boolean;
}

interface NavEnhet {
  navn: string;
  enhetsnummer: string;
  overordnetEnhet: string | null;
}

type RegionMap = Map<string, NavEnhet[]>;

interface Props {
  value: NavEnhet[];
  onChange: (navEnheter: string[]) => void;
  regioner: NavRegion[];
}

export function NavEnhetFilter({ value, onChange, regioner }: Props) {
  const regionMap = buildRegionMap(value);
  const [regionOpen, setRegionOpen] = useState<string[]>([]);

  function regionMapToNavEnheter(regionMap: RegionMap): string[] {
    return Array.from(Object.values(regionMap))
      .flat(1)
      .map((e) => e.enhetsnummer);
  }

  function buildRegionMap(navEnheter: NavEnhet[]): RegionMap {
    const map: RegionMap = new Map<string, NavEnhet[]>();
    navEnheter.forEach((enhet: NavEnhet) => {
      const regionNavn = enhet.overordnetEnhet ?? "unknown";
      const arr = map.get(regionNavn) ?? [];
      arr.push(enhet);
      map.set(regionNavn, arr);
    });

    return map;
  }

  function underenhetCount(region: NavRegion): number {
    return regionMap.get(region.enhetsnummer)?.length ?? 0;
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

    const underenheter = count > 0 ? [] : region.enheter.filter((enhet) => enhet.erStandardvalg);
    onChange(
      regionMapToNavEnheter({
        ...regionMap,
        [region.enhetsnummer]: underenheter,
      }),
    );
  }

  function underenhetIsChecked(enhet: NavEnhet, region: NavRegion): boolean {
    return (regionMap.get(region.enhetsnummer) ?? []).some(
      (e) => e.enhetsnummer === enhet.enhetsnummer,
    );
  }

  function underenhetOnChange(enhet: NavEnhet) {
    onChange(
      addOrRemoveBy(value, enhet, (a, b) => a.enhetsnummer === b.enhetsnummer).map(
        (e) => e.enhetsnummer,
      ),
    );
  }

  return (
    <>
      {regioner.map((region: NavRegion) => (
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
