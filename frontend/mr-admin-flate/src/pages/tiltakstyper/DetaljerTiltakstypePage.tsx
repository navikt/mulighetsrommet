import { Alert, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Link } from "react-router-dom";
import { avtaleTabAtom, AvtaleTabs } from "../../api/atoms";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/Laster";
import { ListLayout } from "../../layouts/ListLayout";
import { AvtalerForTiltakstype } from "./avtaler/AvtalerForTiltakstype";
import "./DetaljerTiltakstypePage.module.scss";
import { TiltakstypeDetaljer } from "./Tiltakstypedetaljer";

export function DetaljerTiltakstypePage() {
  const optionalTiltakstype = useTiltakstypeById();
  const [tabValgt, setTabValgt] = useAtom(avtaleTabAtom);
  const features = useFeatureToggles();

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
      <Header>{tiltakstype.navn}</Header>

      <Tabs
        value={tabValgt}
        onChange={(value) => setTabValgt(value as AvtaleTabs)}
      >
        <Tabs.List>
          <Tabs.Tab value="arenaInfo" label="Arenainfo" />
          {features?.data &&
          features?.data["mulighetsrommet.vis-avtaler-for-tiltakstyper"] ? (
            <Tabs.Tab value="avtaler" label="Avtaler" />
          ) : null}
        </Tabs.List>
        <Tabs.Panel value="arenaInfo" className="h-24 w-full bg-gray-50 p-4">
          <ListLayout>
            <TiltakstypeDetaljer />
          </ListLayout>
        </Tabs.Panel>
        <Tabs.Panel value="avtaler" className="h-24 w-full bg-gray-50 p-4">
          <ListLayout>
            <AvtalerForTiltakstype />
          </ListLayout>
        </Tabs.Panel>
      </Tabs>
    </main>
  );
}
