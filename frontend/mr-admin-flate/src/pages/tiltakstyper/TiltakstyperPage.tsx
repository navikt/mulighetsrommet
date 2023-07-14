import { Heading } from "@navikt/ds-react";
import { Tiltakstypefilter } from "../../components/filter/Tiltakstypefilter";
import { ContainerLayoutOversikt } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import styles from "../Page.module.scss";
import { TiltakstypeTabell } from "../../components/tabell/TiltakstypeTabell";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "../../main";

export function TiltakstyperPage() {
  return (
    <>
      <ErrorBoundary FallbackComponent={ErrorFallback}>
        <div className={styles.header_container}>
          <Heading level="2" size="large" className={styles.header_wrapper}>
            Oversikt over tiltakstyper
          </Heading>
        </div>
        <MainContainer>
          <ContainerLayoutOversikt>
            <Tiltakstypefilter />
            <TiltakstypeTabell />
          </ContainerLayoutOversikt>
        </MainContainer>
      </ErrorBoundary>
    </>
  );
}
