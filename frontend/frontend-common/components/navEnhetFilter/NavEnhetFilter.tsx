import { ChevronDownIcon } from "@navikt/aksel-icons";
import { Checkbox } from "@navikt/ds-react";
import classnames from "classnames";
import { useState } from "react";
import styles from "./NavEnhetFilter.module.scss";
import { addOrRemove, addOrRemoveBy } from "../../utils/utils";

interface NavRegion {
  enhetsnummer: string;
  navn: string;
  enheter: Array<NavRegionUnderenhet>;
}

interface NavRegionUnderenhet {
  navn: string;
  enhetsnummer: string;
  erStandardvalg: boolean;
}

interface NavEnhet {
  navn: string;
  enhetsnummer: string;
}

interface Props {
  value: string[];
  onChange: (navEnheter: string[]) => void;
  regioner: NavRegion[];
}

export function NavEnhetFilter({ value, onChange, regioner }: Props) {
  const [regionOpen, setRegionOpen] = useState<string[]>([]);

  function getSelectedUnderenheter(region: NavRegion): string[] {
    return value.filter((enhetsnummer) =>
      region.enheter.some((enhet) => enhet.enhetsnummer === enhetsnummer),
    );
  }

  function regionIsIndeterminate(region: NavRegion): boolean {
    const count = getSelectedUnderenheter(region).length;
    return count > 0 && count < region.enheter.length;
  }

  function regionIsChecked(region: NavRegion): boolean {
    return getSelectedUnderenheter(region).length === region.enheter.length;
  }

  function regionOnChange(region: NavRegion) {
    const currentlySelectedInRegion = getSelectedUnderenheter(region);

    const nextValue =
      currentlySelectedInRegion.length > 0
        ? value.filter((enhetsnummer) => !currentlySelectedInRegion.includes(enhetsnummer))
        : Array.from(
            new Set(
              region.enheter
                .filter((enhet) => enhet.erStandardvalg)
                .map((enhet) => enhet.enhetsnummer)
                .concat(value),
            ),
          );

    onChange(nextValue);
  }

  function underenhetIsChecked(enhet: NavEnhet): boolean {
    return value.includes(enhet.enhetsnummer);
  }

  function underenhetOnChange(enhet: NavEnhet) {
    onChange(addOrRemoveBy(value, enhet.enhetsnummer, (a, b) => a === b));
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
                  checked={underenhetIsChecked(enhet)}
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
