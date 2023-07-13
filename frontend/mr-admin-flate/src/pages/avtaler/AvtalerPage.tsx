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
    <MainContainer>
      <ContainerLayoutOversikt>
        <Heading level="2" size="large" className={styles.header_wrapper}>
          Oversikt over avtaler
        </Heading>
        <ErrorBoundary FallbackComponent={ErrorFallback}>
          <Tabs defaultValue="avtaler">
            <Tabs.List>
              <Tabs.Tab value="avtaler" label="Avtaler" />
              {features?.["mulighetsrommet.admin-flate-lagre-utkast"] ? (
                <Tabs.Tab
                  data-testid="mine-utkast-tab"
                  value="utkast"
                  label="Mine utkast"
                />
              ) : null}
            </Tabs.List>
            <Tabs.Panel value="avtaler">
              <Avtalefilter />
              <AvtaleTabell />
            </Tabs.Panel>
            <Tabs.Panel value="utkast">
              <AvtaleUtkast />
            </Tabs.Panel>
          </Tabs>
        </ErrorBoundary>
      </ContainerLayoutOversikt>
    </MainContainer>
  );
}
