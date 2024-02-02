import { ChevronDownIcon } from "@navikt/aksel-icons";
import { Accordion, Checkbox, CheckboxGroup } from "@navikt/ds-react";
import classnames from "classnames";
import { useAtom } from "jotai";
import { NavEnhet, NavRegion } from "mulighetsrommet-api-client";
import { useState } from "react";
import { useRegioner } from "../../core/api/queries/useRegioner";
import { filterAccordionAtom } from "../../core/atoms/atoms";
import { RegionMap } from "../../hooks/useArbeidsmarkedstiltakFilter";
import { addOrRemove } from "../../utils/Utils";
import styles from "./NavEnhetFilter.module.scss";

interface Props {
  regionMapFilter: RegionMap;
  setRegionMapFilter: (regionMap: RegionMap) => void;
}

export function NavEnhetFilter({
  regionMapFilter: regionMap,
  setRegionMapFilter: setRegionMap,
}: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(filterAccordionAtom);
  const { data: alleRegioner } = useRegioner();
  const [regionOpen, setRegionOpen] = useState<string[]>([]);

  function regionIsIndeterminate(region: NavRegion): boolean {
    const underenhetCount = regionMap[region.enhetsnummer]?.length ?? 0;
    return underenhetCount > 0 && underenhetCount < region.enheter.length;
  }

  function regionValues(): NavRegion[] {
    return (
      alleRegioner
        ?.filter(
          (region: NavRegion) =>
            Object.keys(regionMap).includes(region.enhetsnummer) &&
            regionMap[region.enhetsnummer].length > 0,
        )
        .filter((region: NavRegion) => !regionIsIndeterminate(region)) ?? []
    );
  }

  function regionOnChange(regioner: NavRegion[]) {
    function enheterAfterChange(region: NavRegion) {
      const isIndeterminate = regionIsIndeterminate(region);
      const isIncluded = regioner.includes(region);

      if (isIndeterminate && !isIncluded) {
        return regionMap[region.enhetsnummer];
      } else if (!isIndeterminate && isIncluded) {
        return region.enheter.map((enhet: NavEnhet) => enhet.enhetsnummer);
      } else {
        return [];
      }
    }

    setRegionMap(
      alleRegioner?.reduce(
        (acc: RegionMap, region: NavRegion) => ({
          ...acc,
          [region.enhetsnummer]: enheterAfterChange(region),
        }),
        {} as RegionMap,
      ) ?? {},
    );
  }

  function underenhetOnChange(region: string, enheter: string[]) {
    setRegionMap({
      ...regionMap,
      [region]: enheter,
    });
  }

  return (
    <Accordion.Item open={accordionsOpen.includes("brukers-enhet")}>
      <Accordion.Header
        onClick={() => {
          setAccordionsOpen([...addOrRemove(accordionsOpen, "brukers-enhet")]);
        }}
        data-testid="filter_accordionheader_brukers-enhet"
      >
        Nav enhet
      </Accordion.Header>
      <Accordion.Content data-testid="filter_accordioncontent_brukers-enhet">
        <CheckboxGroup
          value={regionValues()}
          onChange={regionOnChange}
          legend=""
          hideLegend
          size="small"
          data-testid={"checkboxgroup_brukers-enhet"}
        >
          {alleRegioner?.map((region: NavRegion) => (
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
                      <Checkbox key={enhet.enhetsnummer} value={enhet.enhetsnummer}>
                        {enhet.navn}
                      </Checkbox>
                    ))}
                  </div>
                </CheckboxGroup>
              )}
            </div>
          ))}
        </CheckboxGroup>
      </Accordion.Content>
    </Accordion.Item>
  );
}
