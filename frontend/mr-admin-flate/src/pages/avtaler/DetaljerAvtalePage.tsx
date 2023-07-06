import { Alert, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Link, useParams } from "react-router-dom";
import { AvtaleTabs, avtaleFilter } from "../../api/atoms";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import { Avtalestatus } from "../../components/statuselementer/Avtalestatus";
import { ContainerLayoutDetaljer } from "../../layouts/ContainerLayout";
import commonStyles from "../Page.module.scss";
import { Avtaleinfo } from "./Avtaleinfo";
import { NokkeltallForAvtale } from "./nokkeltall/NokkeltallForAvtale";
import { TiltaksgjennomforingerForAvtale } from "./tiltaksgjennomforinger/TiltaksgjennomforingerForAvtale";

export function DetaljerAvtalePage() {
  const { avtaleId } = useParams<{ avtaleId: string }>();
  if (!avtaleId) {
    throw new Error("Fant ingen avtaleId i url");
  }
  const { data: avtale, isLoading } = useAvtale(avtaleId);
  const [filter, setFilter] = useAtom(avtaleFilter);
  const { data } = useFeatureToggles();

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
        value={filter.avtaleTab}
        onChange={(tab) =>
          setFilter({ ...filter, avtaleTab: tab as AvtaleTabs })
        }
      >
        <Tabs.List className={commonStyles.list}>
          <Tabs.Tab value="avtaleinfo" label="Avtaleinfo" />
          <Tabs.Tab
            data-testid="avtale-tiltaksgjennomforing-tab"
            value="tiltaksgjennomforinger"
            label="Gjennomføringer"
          />
          {data?.["mulighetsrommet.admin-flate-vis-nokkeltall"] ? (
            <Tabs.Tab value="nokkeltall" label="Nøkkeltall" />
          ) : null}
        </Tabs.List>
        <Tabs.Panel value="avtaleinfo">
          <ContainerLayoutDetaljer>
            <Avtaleinfo />
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
