import { Alert, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Link } from "react-router-dom";
import {
  TiltaksgjennomforingerTabs,
  tiltaksgjennomforingTabAtom,
} from "../../api/atoms";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { Laster } from "../../components/laster/Laster";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { TiltaksgjennomforingInfo } from "./TiltaksgjennomforingInfo";
import { NokkeltallForTiltaksgjennomforing } from "./nokkeltall/NokkeltallForTiltaksgjennomforing";
import styles from "./DetaljerTiltaksgjennomforingerPage.module.scss";
import { Header } from "../../components/detaljside/Header";

export function DetaljerTiltaksgjennomforingerPage() {
  const optionalTiltaksgjennomforing = useTiltaksgjennomforingById();
  const [tabValgt, setTabValgt] = useAtom(tiltaksgjennomforingTabAtom);
  const { data } = useFeatureToggles();

  if (
    !optionalTiltaksgjennomforing.data &&
    optionalTiltaksgjennomforing.isLoading
  ) {
    return <Laster tekst="Laster tiltaksgjennømforing" />;
  }

  if (!optionalTiltaksgjennomforing.data) {
    return (
      <Alert variant="warning">
        Klarte ikke finne tiltaksgjennømforing <Link to="/">Til forside</Link>
      </Alert>
    );
  }

  const tiltaksgjennomforing = optionalTiltaksgjennomforing.data;
  return (
    <MainContainer>
      <Header>
        <div className={styles.header}>
          <span>{tiltaksgjennomforing?.navn ?? "..."}</span>
        </div>
      </Header>

      <Tabs
        value={tabValgt}
        onChange={(value) => setTabValgt(value as TiltaksgjennomforingerTabs)}
      >
        <Tabs.List className={styles.list}>
          <Tabs.Tab
            value="detaljer"
            label="Detaljer"
            data-testid="tab_detaljer"
          />
          {data?.["mulighetsrommet.admin-flate-vis-nokkeltall"] ? (
            <Tabs.Tab
              value="nokkeltall"
              label="Nøkkeltall"
              data-testid="tab_nokkeltall"
            />
          ) : null}
        </Tabs.List>
        <Tabs.Panel value="detaljer" className="h-24 w-full bg-gray-50 p-4">
          <ContainerLayout>
            <TiltaksgjennomforingInfo />
          </ContainerLayout>
        </Tabs.Panel>
        <Tabs.Panel value="nokkeltall" className="h-24 w-full bg-gray-50 p-4">
          <ContainerLayout>
            <NokkeltallForTiltaksgjennomforing />
          </ContainerLayout>
        </Tabs.Panel>
      </Tabs>
    </MainContainer>
  );
}
