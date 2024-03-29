import { Tabs } from "@navikt/ds-react";
import React from "react";
import styles from "./FilterAndTableLayout.module.scss";
import { FunnelIcon } from "@navikt/aksel-icons";
import classNames from "classnames";
import { useOutsideClick } from "mulighetsrommet-frontend-common/hooks/useOutsideClick";

interface Props {
  filter: React.ReactNode;
  buttons: React.ReactNode;
  resetButton?: React.ReactNode;
  table: React.ReactNode;
  filterOpen: boolean;
  setFilterOpen: (filterOpen: boolean) => void;
}

export function FilterAndTableLayout({
  filter,
  table,
  resetButton,
  buttons,
  filterOpen,
  setFilterOpen,
}: Props) {
  const ref = useOutsideClick(() => {
    if (window?.innerWidth < 1440) {
      setFilterOpen(false);
    }
  });
  return (
    <div className={styles.filter_table_layout_container}>
      <aside ref={ref}>
        <Tabs
          className={styles.filter_headerbutton}
          size="medium"
          value={filterOpen ? "filter" : ""}
          data-testid="filtertabs"
        >
          <Tabs.List>
            <Tabs.Tab
              className={styles.filtertab}
              onClick={() => setFilterOpen(!filterOpen)}
              value="filter"
              data-testid="filter-tab"
              label="Filter"
              icon={<FunnelIcon title="filter" />}
              aria-controls="filter"
            />
          </Tabs.List>
        </Tabs>
        <div id="filter" className={classNames(styles.filter, !filterOpen && styles.hide_filter)}>
          {filter}
        </div>
      </aside>
      <div className={styles.button_row}>
        <div>
          {resetButton ? <div className={styles.button_row_right}>{resetButton}</div> : null}
        </div>
        <div className={styles.button_row_left}>{buttons}</div>
      </div>
      <div
        className={classNames(
          styles.tags_and_table_container,
          filterOpen
            ? styles.tags_and_table_container_filter_open
            : styles.tags_and_table_container_filter_hidden,
        )}
      >
        <div className={styles.table}>{table}</div>
      </div>
    </div>
  );
}
