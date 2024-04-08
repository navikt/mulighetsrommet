import styles from "./ToolbarButtonRow.module.scss";

interface Props {
  children: React.ReactNode;
}
export const ToolbarButtonRow = ({ children }: Props) => {
  return <div className={styles.button_row}>{children}</div>;
};
