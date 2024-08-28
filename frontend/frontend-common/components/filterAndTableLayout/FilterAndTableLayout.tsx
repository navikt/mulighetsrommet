import React, { Suspense } from "react";
import classNames from "classnames";
import { Filter } from "../filter/Filter";
import { ToolbarButtonRow } from "../toolbar/toolbarButtonRow/ToolbarButtonRow";
import styles from "./FilterAndTableLayout.module.scss";
import { InlineErrorBoundary } from "../error-handling/ErrorBoundary";
import { OversiktSkeleton } from "../skeleton/OversiktSkeleton";
import { FilterContainer } from "../filter/FilterContainer";

interface Props {
  filter: React.ReactNode;
  lagredeFilter?: React.ReactNode;
  buttons: React.ReactNode;
  tags: React.ReactNode;
  table: React.ReactNode;
  filterOpen: boolean;
  setFilterOpen: (filterOpen: boolean) => void;
  nullstillFilterButton: React.ReactNode;
}

export function FilterAndTableLayout({
  filter,
  lagredeFilter,
  buttons,
  tags,
  table,
  filterOpen,
  setFilterOpen,
  nullstillFilterButton,
}: Props) {
  return (
    <Suspense fallback={<OversiktSkeleton />}>
      <div className={styles.filter_table_layout_container}>
        <Filter
          setFilterOpen={setFilterOpen}
          filterOpen={filterOpen}
          filterTab={
            <InlineErrorBoundary>
              <FilterContainer title="Filter" onClose={() => setFilterOpen(false)}>
                {filter}
              </FilterContainer>
            </InlineErrorBoundary>
          }
          lagredeFilterTab={
            lagredeFilter ? (
              <InlineErrorBoundary>
                <FilterContainer title="Lagrede filter" onClose={() => setFilterOpen(false)}>
                  {lagredeFilter}
                </FilterContainer>
              </InlineErrorBoundary>
            ) : null
          }
        />

        <InlineErrorBoundary>
          <ToolbarButtonRow>
            <div className={styles.button_row_left}>{nullstillFilterButton}</div>
            <div className={styles.button_row_right}>{buttons}</div>
          </ToolbarButtonRow>
        </InlineErrorBoundary>

        <div
          className={classNames(
            styles.tags_and_table_container,
            !filterOpen && styles.tags_and_table_container_filter_hidden,
          )}
        >
          <InlineErrorBoundary>
            {tags}
            {table}
          </InlineErrorBoundary>
        </div>
      </div>
    </Suspense>
  );
}
