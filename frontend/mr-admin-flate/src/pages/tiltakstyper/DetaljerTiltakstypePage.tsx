import { Alert, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Link } from "react-router-dom";
import { TiltakstypeAvtaleTabs, tiltakstypeTabAtom } from "../../api/atoms";
import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import { TiltakstypestatusTag } from "../../components/statuselementer/TiltakstypestatusTag";
import { ContainerLayoutDetaljer } from "../../layouts/ContainerLayout";
import commonStyles from "../Page.module.scss";
import { TiltakstypeInfo } from "./TiltakstypeInfo";
import { AvtalerForTiltakstype } from "./avtaler/AvtalerForTiltakstype";

export function DetaljerTiltakstypePage() {
  const optionalTiltakstype = useTiltakstypeById();
  const [tabValgt, setTabValgt] = useAtom(tiltakstypeTabAtom);

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
    <main>
      <Header>
        <div className={commonStyles.header}>
          <span>{tiltakstype?.navn ?? "..."}</span>
          <TiltakstypestatusTag tiltakstype={tiltakstype} />
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
        </Tabs.List>
        <Tabs.Panel value="arenaInfo">
          <ContainerLayoutDetaljer>
            <TiltakstypeInfo />
          </ContainerLayoutDetaljer>
        </Tabs.Panel>
        <Tabs.Panel value="avtaler">
          <ContainerLayoutDetaljer>
            <AvtalerForTiltakstype />
          </ContainerLayoutDetaljer>
        </Tabs.Panel>
      </Tabs>
    </main>
  );
}
