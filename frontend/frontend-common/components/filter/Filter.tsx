import { FunnelIcon, StarIcon } from "@navikt/aksel-icons";
import { Tabs } from "@navikt/ds-react";
import classNames from "classnames";
import React, { useState } from "react";
import { useOutsideClick } from "../../hooks/useOutsideClick";
import styles from "./Filter.module.scss";

interface Props {
  filterTab: React.ReactNode;
  lagredeFilterTab?: React.ReactNode;
  setFilterOpen: (filterOpen: boolean) => void;
  filterOpen: boolean;
}

export function Filter({ filterTab, lagredeFilterTab, setFilterOpen, filterOpen }: Props) {
  const [value, setValue] = useState("filter");
  const ref = useOutsideClick(() => {
    if (window?.innerWidth < 1440) {
      setFilterOpen(false);
    }
  });

  return (
    <div className={styles.filter_container}>
      <aside ref={ref}>
        <Tabs size="medium" className={styles.filter_headerbutton} defaultValue={value}>
          <Tabs.List>
            <Tabs.Tab
              className={styles.filtertab}
              onClick={() => {
                value === "filter" ? setFilterOpen(!filterOpen) : setValue("filter");
                !filterOpen && setFilterOpen(true);
              }}
              value="filter"
              data-testid="filtertab"
              label="Filter"
              icon={<FunnelIcon title="filter" />}
            />
            {lagredeFilterTab && (
              <Tabs.Tab
                className={styles.filtertab}
                onClick={() => {
                  value === "lagrede_filter"
                    ? setFilterOpen(!filterOpen)
                    : setValue("lagrede_filter");
                  !filterOpen && setFilterOpen(true);
                }}
                value="lagrede_filter"
                label="Lagrede filter"
                icon={<StarIcon title="stjerne" />}
              />
            )}
          </Tabs.List>
          <Tabs.Panel
            value="filter"
            className={classNames(styles.filter, !filterOpen && styles.hide_filter)}
          >
            {filterTab}
          </Tabs.Panel>
          {lagredeFilterTab ? (
            <Tabs.Panel
              value="lagrede_filter"
              className={classNames(styles.filter, !filterOpen && styles.hide_filter)}
            >
              {lagredeFilterTab}
            </Tabs.Panel>
          ) : null}
        </Tabs>
      </aside>
    </div>
  );
}
