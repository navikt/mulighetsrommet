import { Accordion, Checkbox, CheckboxGroup, Loader } from "@navikt/ds-react";
import { useState } from "react";
import { addOrRemove } from "../../utils/Utils";
import { filterAccordionAtom } from "../../core/atoms/atoms";
import styles from "./NavEnhetFilter.module.scss";
import { useAtom } from "jotai";
import { NavEnhet, NavRegion } from "mulighetsrommet-api-client";
import { ChevronDownIcon } from "@navikt/aksel-icons";
import classnames from "classnames";
import { RegionMap } from "../../hooks/useArbeidsmarkedstiltakFilter";
import { useRegioner } from "../../core/api/queries/useRegioner";

interface Props {
  regionMapFilter: RegionMap;
  setRegionMapFilter: (regionMap: RegionMap) => void;
}

export function NavEnhetFilter({
  regionMapFilter: regionMap,
  setRegionMapFilter: setRegionMap,
}: Props) {
  const [accordionsOpen, setAccordionsOpen] = useAtom(filterAccordionAtom);
  const { data: alleRegioner, isLoading } = useRegioner();
  const [regionOpen, setRegionOpen] = useState<string[]>([]);

  if (isLoading || !alleRegioner) {
    return <Loader size="xlarge" />;
  }

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
        return region.enheter.map((enhet: NavEnhet) => enhet.enhetsnummer) ?? [];
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

  function RegionCheckbox({ region }: { region: NavRegion }) {
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

  function UnderenheterCheckboxGroup({ region }: { region: NavRegion }) {
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
        <div style={{ maxHeight: "400px", overflow: "auto" }}>
          {region.enheter.map((enhet: NavEnhet) => (
            <Checkbox key={enhet.enhetsnummer} value={enhet.enhetsnummer}>
              {enhet.navn}
            </Checkbox>
          ))}
        </div>
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
          {alleRegioner.map((region: NavRegion) => (
            // eslint-disable-next-line react/prop-types
            <RegionCheckbox key={region.enhetsnummer} region={region} />
          ))}
        </CheckboxGroup>
      </Accordion.Content>
    </Accordion.Item>
  );
}
