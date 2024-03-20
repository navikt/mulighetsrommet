import { ChevronDownIcon } from "@navikt/aksel-icons";
import { Checkbox, CheckboxGroup } from "@navikt/ds-react";
import classnames from "classnames";
import { useState } from "react";
import styles from "./NavEnhetFilter.module.scss";
import { addOrRemove } from "../utils/utils";
import { NavEnhet } from "mulighetsrommet-api-client";
import { NavRegion } from "mulighetsrommet-api-client";

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

  console.log(44, navEnheter);

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

  function regionIsIndeterminate(region: NavRegion): boolean {
    const underenhetCount = regionMap[region.enhetsnummer]?.length ?? 0;
    return underenhetCount > 0 && underenhetCount < region.enheter.length;
  }

  function regionValues(): NavRegion[] {
    return (
      regioner
        ?.filter(
          (region: NavRegion) =>
            Object.keys(regionMap).includes(region.enhetsnummer) &&
            regionMap[region.enhetsnummer].length > 0,
        )
        .filter((region: NavRegion) => !regionIsIndeterminate(region)) ?? []
    );
  }

  function regionOnChange(regioner: NavRegion[]) {
    function enheterAfterChange(region: NavRegion): NavEnhet[] {
      const isIndeterminate = regionIsIndeterminate(region);
      const isIncluded = regioner.includes(region);

      if (isIndeterminate && !isIncluded) {
        return regionMap[region.enhetsnummer];
      } else if (!isIndeterminate && isIncluded) {
        return region.enheter;
      } else {
        return [];
      }
    }

    setNavEnheter(
      regioner?.reduce(
        (acc: NavEnhet[], region: NavRegion) => acc.concat(enheterAfterChange(region)),
        [],
      ) ?? [],
    );
  }

  function underenhetOnChange(region: string, enheter: NavEnhet[]) {
    setNavEnheter(
      regionMapToNavEnheter({
        ...regionMap,
        [region]: enheter,
      }),
    );
  }

  return (
        <CheckboxGroup
          value={regionValues()}
          onChange={regionOnChange}
          legend=""
          hideLegend
          size="small"
          data-testid="checkboxgroup_brukers-enhet"
        >
          {regioner?.map((region: NavRegion) => (
            <div key={region.enhetsnummer}>
              <div
                className={styles.checkbox_and_caret}
                onClick={() => setRegionOpen([...addOrRemove(regionOpen, region.enhetsnummer)])}
              >
                <div onClick={(e) => e.stopPropagation()} className={styles.checkbox}>
                  <Checkbox
                    key={region.enhetsnummer}
                    value={region}
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
                <CheckboxGroup
                  value={regionMap[region.enhetsnummer] ?? []}
                  onChange={(enheter) => underenhetOnChange(region.enhetsnummer, enheter)}
                  key={region.enhetsnummer}
                  legend=""
                  hideLegend
                  size="small"
                  style={{
                    marginLeft: "1rem",
                  }}
                >
                  <div className={styles.underenhet_list}>
                    {region.enheter.map((enhet: NavEnhet) => (
                      <Checkbox key={enhet.enhetsnummer} value={enhet}>
                        {enhet.navn}
                      </Checkbox>
                    ))}
                  </div>
                </CheckboxGroup>
              )}
            </div>
          ))}
        </CheckboxGroup>
  );
}
