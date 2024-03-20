import { Tabs } from "@navikt/ds-react";
import styles from "./Filter.module.scss";
import { FunnelIcon } from "@navikt/aksel-icons";
import classNames from "classnames";
import { useOutsideClick } from "mulighetsrommet-frontend-common/hooks/useOutsideClick";

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
  const ref = useOutsideClick(() => {
    if (window?.innerWidth < 1440) {
      setFilterOpen(false);
    }
  });

  return (
    <div className={styles.container}>
      <div className={styles.button_row}>{buttons}</div>
      <div ref={ref}>
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
        <div id="filter" className={classNames(styles.filter, !filterOpen && styles.hide_filter)}>
          {filter}
        </div>
      </div>
      <div
        className={classNames(styles.tags_and_table_container, !filterOpen && styles.wide_table)}
      >
        {tags}
        <div className={styles.table}>{table}</div>
      </div>
    </div>
  );
}
