import React, { Suspense } from "react";
import classNames from "classnames";
import { FilterSidebar } from "../filter/FilterSidebar";
import { ToolbarButtonRow } from "../toolbar/toolbarButtonRow/ToolbarButtonRow";
import styles from "./FilterAndTableLayout.module.scss";
import { OversiktSkeleton } from "../skeleton/OversiktSkeleton";
import { Box, HStack } from "@navikt/ds-react";

interface Props {
  filter: React.ReactNode;
  buttons: React.ReactNode;
  tags: React.ReactNode;
  table: React.ReactNode;
  filterOpen: boolean;
  setFilterOpen: (filterOpen: boolean) => void;
  nullstillFilterButton: React.ReactNode;
  lagreFilterButton: React.ReactNode;
  hasChanged: boolean;
}

export function FilterAndTableLayout({
  filter,
  buttons,
  tags,
  table,
  filterOpen,
  setFilterOpen,
  nullstillFilterButton,
  lagreFilterButton,
  hasChanged,
}: Props) {
  return (
    <Suspense fallback={<OversiktSkeleton />}>
      <Box background="default" padding="space-8" className={styles.filter_table_layout_container}>
        <FilterSidebar setFilterOpen={setFilterOpen} filterOpen={filterOpen} filterTab={filter} />

        <ToolbarButtonRow>
          {hasChanged &&
            <div className={styles.button_row_left}>
              <HStack align="center" gap="space-4">
                {lagreFilterButton}
                {nullstillFilterButton}
              </HStack>
            </div>
          }
          <div className={styles.button_row_right}>{buttons}</div>
        </ToolbarButtonRow>

        <div
          className={classNames(
            styles.tags_and_table_container,
            !filterOpen && styles.tags_and_table_container_filter_hidden,
          )}
        >
          {tags}
          {table}
        </div>
      </Box>
    </Suspense>
  );
}
