import { Heading, Tabs } from "@navikt/ds-react";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { Laster } from "../../components/Laster";
import { shortcutsForTiltaksansvarlig } from "../../constants";
import { TiltaksgjennomforingerPage } from "../tiltaksgjennomforinger/TiltaksgjennomforingerPage";
import { MineTiltaksgjennomforingerPage } from "../mine/MineTiltaksgjennomforingerPage";
import { EnhetsoversiktPage } from "../enhet/EnhetsoversiktPage";
import styles from "./Forside.module.scss";

export function ForsideTiltaksansvarlig() {
  const { data, isLoading } = useFeatureToggles();

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
    <Tabs defaultValue="/mine" loop>
      <Tabs.List>
        {shortcutsForTiltaksansvarlig.map(({ url, navn }, index) => (
          <Tabs.Tab key={index} value={url} label={navn} />
        ))}
      </Tabs.List>
      <div className={styles.container}>
        <Tabs.Panel value="/mine" className="h-24 w-full bg-gray-50 p-4">
          <MineTiltaksgjennomforingerPage />
        </Tabs.Panel>
        <Tabs.Panel value="/enhet" className="h-24 w-full bg-gray-50 p-4">
          <EnhetsoversiktPage />
        </Tabs.Panel>
        <Tabs.Panel value="/oversikt" className="h-24  w-full bg-gray-50 p-4">
          <TiltaksgjennomforingerPage />
        </Tabs.Panel>
      </div>
    </Tabs>
  );
}
