import { Alert, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Link } from "react-router-dom";
import { avtaleTabAtom, TiltakstypeAvtaleTabs } from "../../api/atoms";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import { Tiltakstypestatus } from "../../components/statuselementer/Tiltakstypestatus";
import {
  ContainerLayoutDetaljer,
  ContainerLayoutTabell,
} from "../../layouts/ContainerLayout";
import { AvtalerForTiltakstype } from "./avtaler/AvtalerForTiltakstype";
import { NokkeltallForTiltakstype } from "./nokkeltall/NokkeltallForTiltakstype";
import { TiltakstypeInfo } from "./TiltakstypeInfo";
import commonStyles from "../Page.module.scss";

export function DetaljerTiltakstypePage() {
  const optionalTiltakstype = useTiltakstypeById();
  const [tabValgt, setTabValgt] = useAtom(avtaleTabAtom);
  const { data } = useFeatureToggles();

  if (!optionalTiltakstype.data && optionalTiltakstype.isLoading) {
    return <Laster tekst="Laster tiltakstype" />;
  }

  if (!optionalTiltakstype.data) {
    return (
      <Alert variant="warning">
        Klarte ikke finne tiltakstype
        <div>
          <Link to="/">Til forside</Link>
        </div>
      </Alert>
    );
  }

  const tiltakstype = optionalTiltakstype.data;
  return (
    <>
      <Header>
        <div className={commonStyles.header}>
          <span>{tiltakstype?.navn ?? "..."}</span>
          <Tiltakstypestatus tiltakstype={tiltakstype} />
        </div>
      </Header>

      <Tabs
        value={tabValgt}
        onChange={(value) => setTabValgt(value as TiltakstypeAvtaleTabs)}
      >
        <Tabs.List className={commonStyles.list}>
          <Tabs.Tab
            value="arenaInfo"
            label="Arenainfo"
            data-testid="tab_arenainfo"
          />
          <Tabs.Tab value="avtaler" label="Avtaler" data-testid="tab_avtaler" />
          {data?.["mulighetsrommet.admin-flate-vis-nokkeltall"] ? (
            <Tabs.Tab
              value="nokkeltall"
              label="Nøkkeltall"
              data-testid="tab_nokkeltall"
            />
          ) : null}
        </Tabs.List>
        <Tabs.Panel value="arenaInfo">
          <ContainerLayoutDetaljer>
            <TiltakstypeInfo />
          </ContainerLayoutDetaljer>
        </Tabs.Panel>
        <Tabs.Panel value="avtaler">
          <ContainerLayoutTabell>
            <AvtalerForTiltakstype />
          </ContainerLayoutTabell>
        </Tabs.Panel>
        <Tabs.Panel value="nokkeltall">
          <ContainerLayoutDetaljer>
            <NokkeltallForTiltakstype />
          </ContainerLayoutDetaljer>
        </Tabs.Panel>
      </Tabs>
    </>
  );
}
