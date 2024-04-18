import classnames from "classnames";
import styles from "./Tabell.module.scss";

interface Props {
  filterOpen?: boolean;
  children: React.ReactNode;
  className?: string;
}
export const TabellWrapper = ({ filterOpen = false, children, className }: Props) => {
  return (
    <div
      className={classnames(styles.tabell_wrapper, className, {
        [styles.tabell_wrapper_filter_open]: filterOpen,
        [styles.tabell_wrapper_filter_hidden]: !filterOpen,
      })}
    >
      {children}
    </div>
  );
};
