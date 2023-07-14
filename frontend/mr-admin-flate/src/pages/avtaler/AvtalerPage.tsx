import { Tabs } from "@navikt/ds-react";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { Avtalefilter } from "../../components/filter/Avtalefilter";
import { AvtaleTabell } from "../../components/tabell/AvtaleTabell";
import { ContainerLayoutOversikt } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import styles from "../Page.module.scss";
import { AvtaleUtkast } from "../../components/avtaler/AvtaleUtkast";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "../../main";
import { HeaderBanner } from "../../layouts/HeaderBanner";

export function AvtalerPage() {
  const { data: features } = useFeatureToggles();
  return (
    <>
      <HeaderBanner heading="Oversikt over avtaler" harUndermeny />
      <ErrorBoundary FallbackComponent={ErrorFallback}>
        <Tabs defaultValue="avtaler">
          <div className={styles.header_container_tabs} role="contentinfo">
            <Tabs.List>
              <Tabs.Tab value="avtaler" label="Avtaler" />
              {features?.["mulighetsrommet.admin-flate-lagre-utkast"] && (
                <Tabs.Tab
                  data-testid="mine-utkast-tab"
                  value="utkast"
                  label="Mine utkast"
                />
              )}
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
