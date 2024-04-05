import classnames from "classnames";
import styles from "./ToolbarContainer.module.scss";

interface Props {
  tagsHeight: number;
  filterOpen: boolean;
  children: React.ReactNode;
}

export const ToolbarContainer = ({ tagsHeight, filterOpen, children }: Props) => {
  return (
    <div
      style={{ top: `calc(${tagsHeight}px + 4.4rem)` }}
      className={classnames(styles.toolbar_container, {
        [styles.toolbar_container_filter_hidden]: !filterOpen,
      })}
    >
      {children}
    </div>
  );
};
