import styles from "./FilterTag.module.scss";
import classNames from "classnames";
import { ReactNode, useLayoutEffect } from "react";
import useResizeObserver from "use-resize-observer";

interface Props {
  children: ReactNode;
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function FilterTagsContainer({ children, filterOpen, setTagsHeight }: Props) {
  const { ref, height = 1 } = useResizeObserver<HTMLDivElement>();

  useLayoutEffect(() => {
    setTagsHeight(height);
  }, [ref, height, setTagsHeight]);

  return (
    <div
      className={classNames(
        styles.filtertags,
        filterOpen ? styles.filtertags_filter_open : styles.filtertags_filter_hidden,
      )}
      data-testid="filtertags"
      ref={ref}
    >
      {children}
    </div>
  );
}
