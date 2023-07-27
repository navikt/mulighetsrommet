import { Alert, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Link } from "react-router-dom";
import { avtaleTabAtom, AvtaleTabs } from "../../api/atoms";
import { useAvtale } from "../../api/avtaler/useAvtale";
import {
  useFeatureToggles,
  VIS_NOKKELTALL_ADMIN_FLATE,
} from "../../api/features/feature-toggles";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import { Avtalestatus } from "../../components/statuselementer/Avtalestatus";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { ContainerLayoutDetaljer } from "../../layouts/ContainerLayout";
import commonStyles from "../Page.module.scss";
import { Avtaleinfo } from "./Avtaleinfo";
import { NokkeltallForAvtale } from "./nokkeltall/NokkeltallForAvtale";
import { TiltaksgjennomforingerForAvtale } from "./tiltaksgjennomforinger/TiltaksgjennomforingerForAvtale";
import NotaterAvtalePage from "../../components/avtaler/NotaterAvtalePage";

export function DetaljerAvtalePage() {
  const avtaleId = useGetAvtaleIdFromUrl();
  if (!avtaleId) {
    throw new Error("Fant ingen avtaleId i url");
  }
  const { data: avtale, isLoading } = useAvtale();
  const [tabValgt, setTabValgt] = useAtom(avtaleTabAtom);
  const features = useFeatureToggles();

  const visNokkeltallFeature =
    features.isSuccess && features.data[VIS_NOKKELTALL_ADMIN_FLATE];

  if (!avtale && isLoading) {
    return (
      <main>
        <Laster tekst="Laster avtale" />
      </main>
    );
  }

  if (!avtale) {
    return (
      <Alert variant="warning">
        Klarte ikke finne avtale
        <div>
          <Link to="/">Til forside</Link>
        </div>
      </Alert>
    );
  }

  return (
    <main>
      <Header>
        <div className={commonStyles.header}>
          <span>{avtale?.navn ?? "..."}</span>
          <Avtalestatus avtale={avtale} />
        </div>
      </Header>
      <Tabs
        value={tabValgt}
        onChange={(tab) => setTabValgt(tab as AvtaleTabs)}
      >
        <Tabs.List className={commonStyles.list}>
          <Tabs.Tab value="avtaleinfo" label="Avtaleinfo" />
          <Tabs.Tab
            value="avtalenotater"
            label="Notater"
            data-testid="tab_avtalenotater"
          />
          <Tabs.Tab
            data-testid="avtale-tiltaksgjennomforing-tab"
            value="tiltaksgjennomforinger"
            label="Gjennomføringer"
          />
          {visNokkeltallFeature ? (
            <Tabs.Tab value="nokkeltall" label="Nøkkeltall" />
          ) : null}
        </Tabs.List>
        <Tabs.Panel value="avtaleinfo">
          <ContainerLayoutDetaljer>
            <Avtaleinfo />
          </ContainerLayoutDetaljer>
        </Tabs.Panel>

        <Tabs.Panel value="avtalenotater">
          <ContainerLayoutDetaljer>
            <NotaterAvtalePage />
          </ContainerLayoutDetaljer>
        </Tabs.Panel>

        <Tabs.Panel value="tiltaksgjennomforinger">
          <ContainerLayoutDetaljer>
            <TiltaksgjennomforingerForAvtale />
          </ContainerLayoutDetaljer>
        </Tabs.Panel>

        <Tabs.Panel value="nokkeltall">
          <ContainerLayoutDetaljer>
            <NokkeltallForAvtale />
          </ContainerLayoutDetaljer>
        </Tabs.Panel>
      </Tabs>
    </main>
  );
}
