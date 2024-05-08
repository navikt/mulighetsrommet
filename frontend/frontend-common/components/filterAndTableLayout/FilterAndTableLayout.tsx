import classNames from "classnames";
import React from "react";
import { Filter } from "../filter/Filter";
import { ToolbarButtonRow } from "../toolbar/toolbarButtonRow/ToolbarButtonRow";
import styles from "./FilterAndTableLayout.module.scss";
import { InlineErrorBoundary } from "../error-handling/ErrorBoundary";

interface Props {
  filter: React.ReactNode;
  buttons: React.ReactNode;
  tags: React.ReactNode;
  table: React.ReactNode;
  filterOpen: boolean;
  setFilterOpen: (filterOpen: boolean) => void;
  nullstillFilterButton: React.ReactNode;
}

export const FilterAndTableLayout = ({
  filter,
  buttons,
  tags,
  table,
  filterOpen,
  setFilterOpen,
  nullstillFilterButton,
}: Props) => {
  return (
    <div className={styles.filter_table_layout_container}>
      <Filter setFilterOpen={setFilterOpen} filterOpen={filterOpen}>
        <InlineErrorBoundary>{filter}</InlineErrorBoundary>
      </Filter>

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
          <>{table}</>
        </InlineErrorBoundary>
      </div>
    </div>
  );
};
