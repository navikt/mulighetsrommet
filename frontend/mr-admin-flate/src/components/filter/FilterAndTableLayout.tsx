import { Tabs } from "@navikt/ds-react";
import styles from "./Filter.module.scss";
import { Separator } from "../detaljside/Metadata";
import { FunnelIcon } from "@navikt/aksel-icons";
import classNames from "classnames";

interface Props {
  filter: React.ReactNode;
  buttons: React.ReactNode;
  tags: React.ReactNode;
  table: React.ReactNode;
  filterOpen: boolean;
  setFilterOpen: (filterOpen: boolean) => void;
}

export function FilterAndTableLayout({
  filter,
  buttons,
  tags,
  table,
  setFilterOpen,
  filterOpen,
}: Props) {
  return (
    <div className={styles.container}>
      <Tabs className={styles.filtertabs} size="medium" value={filterOpen ? "filter" : ""}>
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
      <div className={styles.button_row}>{buttons}</div>
      <div id="filter" className={classNames(styles.filter, !filterOpen && styles.hide_filter)}>
        {filter}
      </div>
      <div
        className={classNames(styles.tags_and_table_container, !filterOpen && styles.wide_table)}
      >
        <Separator style={{ marginBottom: "0.25rem", marginTop: "0" }} />
        {tags}
        <div className={styles.table}>{table}</div>
      </div>
    </div>
  );
}
