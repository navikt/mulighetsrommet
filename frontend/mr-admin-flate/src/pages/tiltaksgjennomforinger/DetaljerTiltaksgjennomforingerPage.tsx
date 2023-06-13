import { Alert, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Link } from "react-router-dom";
import {
  TiltaksgjennomforingerTabs,
  tiltaksgjennomforingTabAtom,
} from "../../api/atoms";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import { Tiltaksgjennomforingstatus } from "../../components/statuselementer/Tiltaksgjennomforingstatus";
import { ContainerLayoutDetaljer } from "../../layouts/ContainerLayout";
import commonStyles from "../Page.module.scss";
import { NokkeltallForTiltaksgjennomforing } from "./nokkeltall/NokkeltallForTiltaksgjennomforing";
import { TiltaksgjennomforingInfo } from "./TiltaksgjennomforingInfo";

export function DetaljerTiltaksgjennomforingerPage() {
  const optionalTiltaksgjennomforing = useTiltaksgjennomforingById();
  const [tabValgt, setTabValgt] = useAtom(tiltaksgjennomforingTabAtom);
  const { data } = useFeatureToggles();

  if (
    !optionalTiltaksgjennomforing.data &&
    optionalTiltaksgjennomforing.isLoading
  ) {
    return <Laster tekst="Laster tiltaksgjennomføring" />;
  }

  if (!optionalTiltaksgjennomforing.data) {
    return (
      <Alert variant="warning">
        Klarte ikke finne tiltaksgjennømforing
        <div>
          <Link to="/">Til forside</Link>
        </div>
      </Alert>
    );
  }

  const tiltaksgjennomforing = optionalTiltaksgjennomforing.data;
  return (
    <main>
      <Header>
        <div className={commonStyles.header}>
          <span>{tiltaksgjennomforing?.navn ?? "..."}</span>
          <Tiltaksgjennomforingstatus
            tiltaksgjennomforing={tiltaksgjennomforing}
          />
        </div>
      </Header>

      <Tabs
        value={tabValgt}
        onChange={(value) => setTabValgt(value as TiltaksgjennomforingerTabs)}
      >
        <Tabs.List className={commonStyles.list}>
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
        <Tabs.Panel value="detaljer">
          <ContainerLayoutDetaljer>
            <TiltaksgjennomforingInfo />
          </ContainerLayoutDetaljer>
        </Tabs.Panel>
        <Tabs.Panel value="nokkeltall">
          <ContainerLayoutDetaljer>
            <NokkeltallForTiltaksgjennomforing />
          </ContainerLayoutDetaljer>
        </Tabs.Panel>
      </Tabs>
    </main>
  );
}
