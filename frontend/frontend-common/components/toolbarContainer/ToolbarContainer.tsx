import classnames from "classnames";
import styles from "./ToolbarContainer.module.scss";

interface Props {
  tagsHeight: number;
  filterOpen: boolean;
  children: React.ReactNode;
  className?: string;
}
export const ToolbarContainer = ({ tagsHeight, filterOpen, children, className }: Props) => {
  return (
    <div
      style={{ top: `calc(${tagsHeight}px + 4.4rem)` }}
      className={classnames(styles.toolbar_container, className, {
        [styles.toolbar_container_filter_open]: filterOpen,
        [styles.toolbar_container_filter_hidden]: !filterOpen,
      })}
    >
      {children}
    </div>
  );
};
