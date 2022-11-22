import { App } from "../App";
import { AdministratorHeader } from "../components/AdministratorHeader";
import styles from "./RootLayout.module.scss";

export function RootLayout() {
  return (
    <div>
      <AdministratorHeader />
      <main className={styles.container}>
        <App />
      </main>
    </div>
  );
}
