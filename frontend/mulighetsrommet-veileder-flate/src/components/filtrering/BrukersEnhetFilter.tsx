import { Accordion, Checkbox, CheckboxGroup, Loader } from "@navikt/ds-react";
import { useState } from "react";
import { addOrRemove } from "../../utils/Utils";
import { filterAccordionAtom } from "../../core/atoms/atoms";
import styles from "./BrukersEnhetFilter.module.scss";
import { useAtom } from "jotai";
import { NavEnhet } from "mulighetsrommet-api-client";
import { ChevronDownIcon } from "@navikt/aksel-icons";
import classnames from "classnames";
import { useRegionMap } from "../../core/api/queries/useRegionMap";
import { RegionMap } from "../../hooks/useArbeidsmarkedstiltakFilter";

interface Props {
  regionMapFilter: RegionMap;
  setRegionMapFilter: (regionMap: RegionMap) => void;
}

export function BrukersEnhetFilter({
  regionMapFilter: regionMap,
  setRegionMapFilter: setRegionMap,
}: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(filterAccordionAtom);
  const { data: fullRegionMap, isLoading } = useRegionMap();
  const [regionOpen, setRegionOpen] = useState<string[]>([]);
  if (isLoading || !fullRegionMap) {
    return <Loader size="xlarge" />;
  }

  function regionIsIndeterminate(region: NavEnhet): boolean {
    const underenhetCount = regionMap[region.enhetsnummer]?.length ?? 0;
    return underenhetCount > 0 && underenhetCount < (fullRegionMap.get(region)?.length ?? 0);
  }

  function regionValues(): NavEnhet[] {
    return Array.from(fullRegionMap.keys())
      .filter(
        (region: NavEnhet) =>
          Object.keys(regionMap).includes(region.enhetsnummer) &&
          regionMap[region.enhetsnummer].length > 0,
      )
      .filter((region: NavEnhet) => !regionIsIndeterminate(region));
  }

  function regionOnChange(regioner: NavEnhet[]) {
    function enheterAfterChange(region: NavEnhet) {
      const isIndeterminate = regionIsIndeterminate(region);
      const isIncluded = regioner.includes(region);

      if (isIndeterminate && !isIncluded) {
        return regionMap[region.enhetsnummer];
      } else if (!isIndeterminate && isIncluded) {
        return fullRegionMap.get(region)?.map((enhet: NavEnhet) => enhet.enhetsnummer) ?? [];
      } else {
        return [];
      }
    }

    setRegionMap(
      Array.from(fullRegionMap.keys()).reduce(
        (acc: RegionMap, region: NavEnhet) => ({
          ...acc,
          [region.enhetsnummer]: enheterAfterChange(region),
        }),
        {} as RegionMap,
      ),
    );
  }

  function RegionCheckbox({ region }: { region: NavEnhet }) {
    return (
      <>
        <div
          className={styles.checkbox_and_caret}
          onClick={() => setRegionOpen([...addOrRemove(regionOpen, region.enhetsnummer)])}
        >
          <div onClick={(e) => e.stopPropagation()}>
            <Checkbox
              key={region.enhetsnummer}
              value={region}
              indeterminate={regionIsIndeterminate(region)}
            >
              {region.navn}
            </Checkbox>
          </div>
          <ChevronDownIcon
            fontSize="1.25rem"
            className={classnames(styles.accordion_down, {
              [styles.accordion_down_open]: regionOpen.includes(region.enhetsnummer),
            })}
          />
        </div>
        {regionOpen.includes(region.enhetsnummer) && <UnderenheterCheckboxGroup region={region} />}
      </>
    );
  }

  function underenhetOnChange(region: string, enheter: string[]) {
    setRegionMap({
      ...regionMap,
      [region]: enheter,
    });
  }

  function UnderenheterCheckboxGroup({ region }: { region: NavEnhet }) {
    return (
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
        {(fullRegionMap.get(region) ?? []).map((enhet: NavEnhet) => (
          <Checkbox key={enhet.enhetsnummer} value={enhet.enhetsnummer}>
            {enhet.navn}
          </Checkbox>
        ))}
      </CheckboxGroup>
    );
  }

  return (
    <Accordion.Item open={accordionsOpen.includes("brukers-enhet")}>
      <Accordion.Header
        onClick={() => {
          setAccordionsOpen([...addOrRemove(accordionsOpen, "brukers-enhet")]);
        }}
        data-testid="filter_accordionheader_brukers-enhet"
      >
        Brukers enhet
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
          {Array.from(fullRegionMap.keys()).map((region: NavEnhet) => (
            // eslint-disable-next-line react/prop-types
            <RegionCheckbox key={region.enhetsnummer} region={region} />
          ))}
        </CheckboxGroup>
      </Accordion.Content>
    </Accordion.Item>
  );
}
