import styles from "./FilterTag.module.scss";
import classNames from "classnames";
import { ReactNode, useLayoutEffect, useRef } from "react";
import { useHeightObserver } from "../../../hooks/useHeightObserver";

interface Props {
  children: ReactNode;
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function FilterTagsContainer({ children, filterOpen, setTagsHeight }: Props) {
  const ref = useRef<HTMLDivElement | null>(null);
  const height = useHeightObserver<HTMLDivElement>(ref);

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
