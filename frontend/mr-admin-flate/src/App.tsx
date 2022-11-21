import { Heading } from "@navikt/ds-react";
import { useFeatureToggles } from "./api/features/feature-toggles";
import { Oversikt } from "./pages/Oversikt";
import styles from "./App.module.scss";

export function App() {
  const { data, isLoading } = useFeatureToggles();

  if (!data || isLoading) return null;

  if (!data["mulighetsrommet.enable-admin-flate"]) {
    return (
      <Heading data-testid="admin-heading" size="xlarge">
        Admin-flate er skrudd av 💤
      </Heading>
    );
  }

  return (
    <main className={styles.app_container}>
      <Heading data-testid="admin-heading" size="xlarge">
        Hello World, admin-flate 💯
      </Heading>
      <Oversikt />
    </main>
  );
}
