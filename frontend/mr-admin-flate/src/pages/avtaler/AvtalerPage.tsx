import { Heading, Tabs } from "@navikt/ds-react";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { Avtalefilter } from "../../components/filter/Avtalefilter";
import { AvtaleTabell } from "../../components/tabell/AvtaleTabell";
import { ContainerLayoutOversikt } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import styles from "../Page.module.scss";
import { AvtaleUtkast } from "../../components/avtaler/AvtaleUtkast";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "../../main";

export function AvtalerPage() {
  const { data: features } = useFeatureToggles();
  return (
    <>
      <ErrorBoundary FallbackComponent={ErrorFallback}>
        <div className={styles.header_container}>
          <Heading level="2" size="large" className={styles.header_wrapper}>
            Oversikt over avtaler
          </Heading>
        </div>
        <Tabs defaultValue="avtaler">
          <div className={styles.header_container_tabs}>
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
