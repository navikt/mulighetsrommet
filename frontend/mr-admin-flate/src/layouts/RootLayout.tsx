import { App } from "../App";
import styles from "./RootLayout.module.scss";

export function RootLayout() {
  return (
    <div className={styles.container}>
      <App />
    </div>
  );
}
