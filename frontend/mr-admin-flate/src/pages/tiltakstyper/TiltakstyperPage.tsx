import { Heading } from "@navikt/ds-react";
import { Tiltakstypefilter } from "../../components/filter/Tiltakstypefilter";
import { ContainerLayoutOversikt } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import styles from "../Page.module.scss";
import { TiltakstypeTabell } from "../../components/tabell/TiltakstypeTabell";

export function TiltakstyperPage() {
  return (
    <MainContainer>
      <ContainerLayoutOversikt>
        <Heading level="2" size="large" className={styles.header_wrapper}>
          Oversikt over tiltakstyper
        </Heading>
        <Tiltakstypefilter />
        <TiltakstypeTabell />
      </ContainerLayoutOversikt>
    </MainContainer>
  );
}
