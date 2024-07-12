import { FunnelIcon } from "@navikt/aksel-icons";
import { Tabs } from "@navikt/ds-react";
import classNames from "classnames";
import React from "react";
import { useOutsideClick } from "../../hooks/useOutsideClick";
import styles from "./Filter.module.scss";

interface Props {
  children: React.ReactNode;
  setFilterOpen: (filterOpen: boolean) => void;
  filterOpen: boolean;
}

export function Filter({ children, setFilterOpen, filterOpen }: Props) {
  const ref = useOutsideClick(() => {
    if (window?.innerWidth < 1440) {
      setFilterOpen(false);
    }
  });

  return (
    <div className={styles.filter_container}>
      <aside ref={ref}>
        <Tabs
          size="medium"
          value={filterOpen ? "filter" : ""}
          className={styles.filter_headerbutton}
        >
          <Tabs.List>
            <Tabs.Tab
              className={styles.filtertab}
              onClick={() => setFilterOpen(!filterOpen)}
              value="filter"
              data-testid="filtertab"
              label="Filter"
              icon={<FunnelIcon title="filter" />}
              aria-controls="filter"
            />
          </Tabs.List>
        </Tabs>
        <div id="filter" className={classNames(styles.filter, !filterOpen && styles.hide_filter)}>
          {children}
        </div>
      </aside>
    </div>
  );
}
