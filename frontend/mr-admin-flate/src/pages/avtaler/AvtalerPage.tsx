import { Tabs } from "@navikt/ds-react";
import { ErrorBoundary } from "react-error-boundary";
import { AvtaleUtkast } from "../../components/avtaler/AvtaleUtkast";
import { Avtalefilter } from "../../components/filter/Avtalefilter";
import { AvtaleTabell } from "../../components/tabell/AvtaleTabell";
import { ContainerLayoutOversikt } from "../../layouts/ContainerLayout";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { MainContainer } from "../../layouts/MainContainer";
import { ErrorFallback } from "../../main";
import styles from "../Page.module.scss";

export function AvtalerPage() {
  return (
    <>
      <HeaderBanner heading="Oversikt over avtaler" harUndermeny />
      <ErrorBoundary FallbackComponent={ErrorFallback}>
        <Tabs defaultValue="avtaler">
          <div className={styles.header_container_tabs} role="contentinfo">
            <Tabs.List>
              <Tabs.Tab value="avtaler" label="Avtaler" />
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
                <AvtaleUtkast />
              </Tabs.Panel>
            </ContainerLayoutOversikt>
          </MainContainer>
        </Tabs>
      </ErrorBoundary>
    </>
  );
}
