import { ChevronDownIcon, FunnelIcon, XMarkIcon } from "@navikt/aksel-icons";
import { Box, Heading, HStack } from "@navikt/ds-react";
import classNames from "classnames";
import { ReactNode, useCallback, useState } from "react";
import { useOutsideClick } from "../../hooks/useOutsideClick";
import styles from "./Filter.module.scss";

interface Props {
  filterTab: ReactNode;
  setFilterOpen: (filterOpen: boolean) => void;
  filterOpen: boolean;
}

export function Filter({ filterTab, setFilterOpen, filterOpen }: Props) {
  const [activeTab, setActiveTab] = useState("filter");

  const closeFilter = useCallback(() => {
    if (window.innerWidth < 1440) {
      setFilterOpen(false);
    }
  }, [setFilterOpen]);
  const ref = useOutsideClick(closeFilter);

  return (
    <div className={styles.filter_container}>
      <aside ref={ref}>
        <Box
          data-testid="filterbox"
          className="row-start-1 col-start-1 w-full border-b-3 cursor-pointer bg-ax-bg-default hover:bg-ax-bg-neutral-soft pt-4 pl-4 pr-4 pb-1"
          borderColor="accent"
          onClick={() => {
            if (activeTab === "filter") {
              setFilterOpen(!filterOpen);
            } else {
              setActiveTab("filter");
            }
            if (!filterOpen) {
              setFilterOpen(true);
            }
          }}
        >
          <HStack justify="space-between" align="center">
            <HStack gap="space-8" align="center">
              <FunnelIcon fontSize="1.5rem" title="filter" />
              <Heading level="2" size="small">
                Filter
              </Heading>
            </HStack>
            {filterOpen ? (
              <XMarkIcon aria-label="Kryss for å lukke filter" />
            ) : (
              <ChevronDownIcon title="Åpne filter" />
            )}
          </HStack>
        </Box>
        <div className={classNames(styles.filter, !filterOpen && styles.hide_filter)}>
          {filterTab}
        </div>
      </aside>
    </div>
  );
}
