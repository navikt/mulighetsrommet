import { Alert, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Link } from "react-router-dom";
import { AvtaleTabs, avtaleFilter } from "../../api/atoms";
import { useAvtale } from "../../api/avtaler/useAvtale";
import NotaterAvtalePage from "../../components/avtaler/NotaterAvtalePage";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import { Avtalestatus } from "../../components/statuselementer/Avtalestatus";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { ContainerLayoutDetaljer } from "../../layouts/ContainerLayout";
import commonStyles from "../Page.module.scss";
import { Avtaleinfo } from "./Avtaleinfo";
import { TiltaksgjennomforingerForAvtale } from "./tiltaksgjennomforinger/TiltaksgjennomforingerForAvtale";

export function DetaljerAvtalePage() {
  const avtaleId = useGetAvtaleIdFromUrl();
  if (!avtaleId) {
    throw new Error("Fant ingen avtaleId i url");
  }
  const { data: avtale, isLoading } = useAvtale();
  const [filter, setFilter] = useAtom(avtaleFilter);

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
            value="avtalenotater"
            label="Notater"
            data-testid="tab_avtalenotater"
          />
          <Tabs.Tab
            data-testid="avtale-tiltaksgjennomforing-tab"
            value="tiltaksgjennomforinger"
            label="Gjennomføringer"
          />
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
      </Tabs>
    </main>
  );
}
