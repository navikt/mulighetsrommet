import { Tabs } from "@navikt/ds-react";
import { ErrorBoundary } from "react-error-boundary";
import { Avtalefilter } from "../../components/filter/Avtalefilter";
import { AvtaleTabell } from "../../components/tabell/AvtaleTabell";
import { ContainerLayoutOversikt } from "../../layouts/ContainerLayout";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { MainContainer } from "../../layouts/MainContainer";
import { ErrorFallback } from "../../main";
import styles from "../Page.module.scss";
import { avtaleOversiktTabAtom, AvtaleUtkastTabs } from "../../api/atoms";
import { useAtom } from "jotai";
import { UtkastListe } from "../../components/utkast/Utkastliste";

export function AvtalerPage() {
  const [tabValgt, setTabValgt] = useAtom(avtaleOversiktTabAtom);

  return (
    <>
      <HeaderBanner heading="Oversikt over avtaler" harUndermeny />
      <ErrorBoundary FallbackComponent={ErrorFallback}>
        <Tabs
          value={tabValgt}
          defaultValue="avtaler"
          onChange={(value) => setTabValgt(value as AvtaleUtkastTabs)}
        >
          <div className={styles.header_container_tabs} role="contentinfo">
            <Tabs.List>
              <Tabs.Tab
                data-testid="avtaler-tab"
                value="avtaler"
                label="Avtaler"
              />
              <Tabs.Tab
                data-testid="mine-utkast-tab"
                value="utkast"
                label="Mine utkast"
              />
            </Tabs.List>
          </div>
          <MainContainer>
            <ContainerLayoutOversikt>
              <Tabs.Panel value="avtaler">
                <Avtalefilter />
                <AvtaleTabell />
              </Tabs.Panel>
              <Tabs.Panel value="utkast">
                <UtkastListe dataType="avtale" />
              </Tabs.Panel>
            </ContainerLayoutOversikt>
          </MainContainer>
        </Tabs>
      </ErrorBoundary>
    </>
  );
}
