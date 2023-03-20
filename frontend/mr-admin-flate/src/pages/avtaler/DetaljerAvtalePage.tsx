import { Tabs } from "@navikt/ds-react";
import { useState } from "react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { Header } from "../../components/detaljside/Header";
import { Avtalestatus } from "../../components/statuselementer/Avtalestatus";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import { Avtaleinfo } from "./Avtaleinfo";
import { NokkeltallForAvtale } from "./nokkeltall/NokkeltallForAvtale";
import styles from "./DetaljerAvtalePage.module.scss";

export function DetaljerAvtalePage() {
  const { data: avtale } = useAvtale();
  const [tabValgt, setTabValgt] = useState("avtaleinfo");
  const { data } = useFeatureToggles();

  if (!avtale) {
    return null;
  }

  return (
    <MainContainer>
      <Header>
        <div className={styles.header}>
          <span>{avtale?.navn ?? "..."}</span>
          <Avtalestatus avtale={avtale} />
        </div>{" "}
      </Header>
      <Tabs value={tabValgt} onChange={setTabValgt}>
        <Tabs.List className={styles.list}>
          <Tabs.Tab value="avtaleinfo" label="Avtaleinfo" />
          {data?.["mulighetsrommet.admin-flate-vis-nokkeltall"] ? (
            <Tabs.Tab value="nokkeltall" label="Nøkkeltall" />
          ) : null}
        </Tabs.List>
        <Tabs.Panel value="avtaleinfo" className="h-24 w-full bg-gray-50 p-4">
          <ContainerLayout>
            <Avtaleinfo />
          </ContainerLayout>
        </Tabs.Panel>
        <Tabs.Panel value="nokkeltall" className="h-24 w-full bg-gray-50 p-4">
          <ContainerLayout>
            <NokkeltallForAvtale />
          </ContainerLayout>
        </Tabs.Panel>
      </Tabs>
    </MainContainer>
  );
}
