import { Heading } from "@navikt/ds-react";
import { Tiltaksgjennomforingfilter } from "../../components/filter/Tiltaksgjennomforingfilter";
import { ContainerLayoutOversikt } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import styles from "../Page.module.scss";
import { TiltaksgjennomforingsTabell } from "../../components/tabell/TiltaksgjennomforingsTabell";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "../../main";

export function TiltaksgjennomforingerPage() {
  return (
    <>
      <ErrorBoundary FallbackComponent={ErrorFallback}>
        <div className={styles.header_container}>
          <Heading level="2" size="large" className={styles.header_wrapper}>
            Oversikt over tiltaksgjennomføringer
          </Heading>
        </div>
        <MainContainer>
          <ContainerLayoutOversikt>
            <Tiltaksgjennomforingfilter />
            <TiltaksgjennomforingsTabell />
          </ContainerLayoutOversikt>
        </MainContainer>
      </ErrorBoundary>
    </>
  );
}
