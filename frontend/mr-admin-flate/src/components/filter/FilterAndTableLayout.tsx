import { Popover, Tabs } from "@navikt/ds-react";
import { useRef, useState } from "react";
import styles from "./Filter.module.scss";
import { Separator } from "../detaljside/Metadata";
import { FunnelIcon } from "@navikt/aksel-icons";

interface Props {
  filter: React.ReactNode;
  buttons: React.ReactNode;
  tags: React.ReactNode;
  table: React.ReactNode;
}

export function FilterAndTableLayout(props: Props) {
  const { filter, table, buttons, tags } = props;
  const [filterSelected, setFilterSelected] = useState<boolean>(true);

  const filterTabRef = useRef<HTMLDivElement>(null);

  return (
    <div className={styles.container}>
      <Tabs className={styles.filter_tabs} size="medium" value={filterSelected ? "filter" : ""}>
        <Tabs.List>
          <Tabs.Tab
            className={styles.filter_tab}
            ref={filterTabRef}
            onClick={() => setFilterSelected(!filterSelected)}
            value="filter"
            label="Filter"
            icon={<FunnelIcon title="filter" />}
            aria-controls="popover"
          />
        </Tabs.List>
      </Tabs>
      <div className={styles.button_row}>{buttons}</div>
      <Popover
        id="popover"
        className={styles.filter}
        open={filterSelected}
        style={undefined}
        onClose={() => {}}
        arrow={false}
        anchorEl={filterTabRef.current}
      >
        <Popover.Content className={styles.filter_content}>{filter}</Popover.Content>
      </Popover>
      <div className={styles.tags_and_table_container}>
        <Separator style={{ marginBottom: "0.25rem", marginTop: "0" }} />
        {tags}
        <div className={styles.table}>{table}</div>
      </div>
    </div>
  );
}
