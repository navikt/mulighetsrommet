import { Tabs } from "@navikt/ds-react";
import React, { useState } from "react";
import styles from "./FilterAndTableLayout.module.scss";
import { FunnelIcon } from "@navikt/aksel-icons";
import classNames from "classnames";
import { Separator } from "../../utils/Separator";

interface Props {
  filter: React.ReactNode;
  buttons: React.ReactNode;
  resetButton?: React.ReactNode;
  tags: React.ReactNode;
  table: React.ReactNode;
}

export function FilterAndTableLayout(props: Props) {
  const { filter, table, resetButton, buttons, tags } = props;
  const [filterSelected, setFilterSelected] = useState<boolean>(true);

  return (
    <div className={styles.filter_table_layout_container}>
      <Tabs
        className={styles.filter_tabs}
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
          !filterSelected && styles.wide_table,
        )}
      >
        <Separator providedStyle={{ marginBottom: "0.25rem", marginTop: "0" }} />
        {tags}
        <div className={styles.table}>{table}</div>
      </div>
    </div>
  );
}
