import { Tabs } from "@navikt/ds-react";
import { useState } from "react";
import styles from "./Filter.module.scss";
import { Separator } from "../detaljside/Metadata";
import { FunnelIcon } from "@navikt/aksel-icons";
import classNames from "classnames";
import { useOutsideClick } from "mulighetsrommet-frontend-common/hooks/useOutsideClick";

interface Props {
  filter: React.ReactNode;
  buttons: React.ReactNode;
  tags: React.ReactNode;
  table: React.ReactNode;
}

export function FilterAndTableLayout(props: Props) {
  const { filter, table, buttons, tags } = props;
  const [filterSelected, setFilterSelected] = useState<boolean>(true);
  const ref = useOutsideClick(() => {
    if (window?.innerWidth < 1440) {
      setFilterSelected(false);
    }
  });

  return (
    <div className={styles.container}>
      <Tabs className={styles.filter_tabs} size="medium" value={filterSelected ? "filter" : ""}>
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
      <div className={styles.button_row}>{buttons}</div>
      <div
        ref={ref}
        id="filter"
        className={classNames(styles.filter, !filterSelected && styles.hide_filter)}
      >
        {filter}
      </div>
      <div
        className={classNames(
          styles.tags_and_table_container,
          !filterSelected && styles.wide_table,
        )}
      >
        <Separator style={{ marginBottom: "0.25rem", marginTop: "0" }} />
        {tags}
        <div className={styles.table}>{table}</div>
      </div>
    </div>
  );
}
