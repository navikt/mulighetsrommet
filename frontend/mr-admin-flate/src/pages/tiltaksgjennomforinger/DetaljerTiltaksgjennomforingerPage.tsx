import { Alert, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Link } from "react-router-dom";
import {
  TiltaksgjennomforingerTabs,
  tiltaksgjennomforingTabAtom,
} from "../../api/atoms";
import {
  VIS_DELTAKERLISTE_KOMET,
  useFeatureToggles,
} from "../../api/features/feature-toggles";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import { Tiltaksgjennomforingstatus } from "../../components/statuselementer/Tiltaksgjennomforingstatus";
import NotaterTiltaksgjennomforingerPage from "../../components/tiltaksgjennomforinger/NotaterTiltaksgjennomforingerPage";
import { ContainerLayoutDetaljer } from "../../layouts/ContainerLayout";
import { DeltakerListe } from "../../microfrontends/team_komet/Deltakerliste";
import commonStyles from "../Page.module.scss";
import { TiltaksgjennomforingInfo } from "./TiltaksgjennomforingInfo";

export function DetaljerTiltaksgjennomforingerPage() {
  const optionalTiltaksgjennomforing = useTiltaksgjennomforingById();
  const [tabValgt, setTabValgt] = useAtom(tiltaksgjennomforingTabAtom);
  const features = useFeatureToggles();

  const visDeltakerlisteFraKometFeature =
    features.isSuccess && features.data[VIS_DELTAKERLISTE_KOMET];

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

          <Tabs.Tab value="tiltaksgjennomforingsnotater" label="Notater" />

          {visDeltakerlisteFraKometFeature ? (
            <Tabs.Tab value="poc" label="Deltakerliste" />
          ) : null}
        </Tabs.List>

        <Tabs.Panel value="detaljer">
          <ContainerLayoutDetaljer>
            <TiltaksgjennomforingInfo />
          </ContainerLayoutDetaljer>
        </Tabs.Panel>

        <Tabs.Panel value="tiltaksgjennomforingsnotater">
          <ContainerLayoutDetaljer>
            <NotaterTiltaksgjennomforingerPage />
          </ContainerLayoutDetaljer>
        </Tabs.Panel>

        <Tabs.Panel value="poc">
          <ContainerLayoutDetaljer>
            <DeltakerListe />
          </ContainerLayoutDetaljer>
        </Tabs.Panel>
      </Tabs>
    </main>
  );
}
