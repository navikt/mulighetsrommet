import { Tabs } from "@navikt/ds-react";
import React from "react";
import styles from "./FilterAndTableLayout.module.scss";
import { FunnelIcon } from "@navikt/aksel-icons";
import classNames from "classnames";

interface Props {
  filter: React.ReactNode;
  buttons: React.ReactNode;
  resetButton?: React.ReactNode;
  table: React.ReactNode;
  filterSelected: boolean;
  setFilterSelected: (filterSelected: boolean) => void;
}

export function FilterAndTableLayout({
  filter,
  table,
  resetButton,
  buttons,
  filterSelected,
  setFilterSelected,
}: Props) {
  return (
    <div className={styles.filter_table_layout_container}>
      <Tabs
        className={styles.filter_headerbutton}
        size="medium"
        value={filterSelected ? "filter" : ""}
        data-testid="filter_tabs"
      >
        <Tabs.List>
          <Tabs.Tab
            className={styles.filter_tab}
            onClick={() => setFilterSelected(!filterSelected)}
            value="filter"
            data-testid="filter-tab"
            label="Filter"
            icon={<FunnelIcon title="filter" />}
            aria-controls="filter"
          />
        </Tabs.List>
      </Tabs>
      <div className={styles.button_row}>
        {resetButton ? <div className={styles.button_row_right}>{resetButton}</div> : <div></div>}
        <div className={styles.button_row_left}>{buttons}</div>
      </div>
      <div id="filter" className={classNames(styles.filter, !filterSelected && styles.hide_filter)}>
        {filter}
      </div>
      <div
        className={classNames(
          styles.tags_and_table_container,
          filterSelected
            ? styles.tags_and_table_container_filter_selected
            : styles.tags_and_table_container_filter_unselected,
        )}
      >
        <div className={styles.table}>{table}</div>
      </div>
    </div>
  );
}
