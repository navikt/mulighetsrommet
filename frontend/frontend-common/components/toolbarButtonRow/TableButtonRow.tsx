import styles from "./TableButtonRow.module.scss";

interface Props {
  children: React.ReactNode;
}
export const TableButtonRow = ({ children }: Props) => {
  return <div className={styles.button_row}>{children}</div>;
};
