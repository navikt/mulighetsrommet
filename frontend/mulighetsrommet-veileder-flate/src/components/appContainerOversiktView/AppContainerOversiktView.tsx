import styles from "./AppContainerOversiktView.module.scss";

interface Props {
  children: React.ReactNode;
  header?: React.ReactNode;
}

export const AppContainerOversiktView = ({ children, header }: Props) => {
  return (
    <div className={styles.app_container}>
      {header}
      <div className={styles.app_content}>{children}</div>
    </div>
  );
};
