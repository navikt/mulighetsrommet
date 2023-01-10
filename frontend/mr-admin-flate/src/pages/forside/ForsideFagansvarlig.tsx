import { Heading, Tabs } from "@navikt/ds-react";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { Laster } from "../../components/Laster";
import { shortcutsForFagansvarlig } from "../../constants";
import { TiltakstyperPage } from "../tiltakstyper/TiltakstyperPage";
import styles from "./Forside.module.scss";
import { useState } from "react";

export function ForsideFagansvarlig() {
  const { data, isLoading } = useFeatureToggles();
  const [state, setState] = useState("/tiltakstyper");

  if (isLoading) return <Laster size="xlarge" />;

  if (!data) return null;

  if (!data["mulighetsrommet.enable-admin-flate"]) {
    return (
      <Heading data-testid="admin-heading" size="xlarge">
        Admin-flate er skrudd av 💤
      </Heading>
    );
  }

  return (
    <Tabs value={state} onChange={setState} loop>
      <Tabs.List>
        {shortcutsForFagansvarlig.map(({ url, navn }, index) => (
          <Tabs.Tab key={index} value={url} label={navn} />
        ))}
      </Tabs.List>
      <div className={styles.container}>
        <Tabs.Panel
          value="/tiltakstyper"
          className="h-24 w-full bg-gray-50 p-4"
        >
          <TiltakstyperPage />
        </Tabs.Panel>
      </div>
    </Tabs>
  );
}
