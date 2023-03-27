import { Heading } from "@navikt/ds-react";
import { Tiltakstypefilter } from "../../components/filter/Tiltakstypefilter";
import { TiltakstyperOversikt } from "../../components/tiltakstyper/TiltakstyperOversikt";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import { NavigeringHeader } from "../forside/NavigeringHeader";
import styles from "../Page.module.scss";

export function TiltakstyperPage() {
  return (
    <MainContainer>
      <NavigeringHeader />
      <ContainerLayout>
        <Heading level="2" size="large" className={styles.header_wrapper}>
          Oversikt over tiltakstyper
        </Heading>
        <Tiltakstypefilter />
        <TiltakstyperOversikt />
      </ContainerLayout>
    </MainContainer>
  );
}
