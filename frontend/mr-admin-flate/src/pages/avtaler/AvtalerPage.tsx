import { Heading } from "@navikt/ds-react";
import { Avtalefilter } from "../../components/filter/Avtalefilter";
import { AvtaleTabell } from "../../components/tabell/AvtaleTabell";
import { ContainerLayoutOversikt } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import styles from "../Page.module.scss";

export function AvtalerPage() {
  return (
    <MainContainer>
      <ContainerLayoutOversikt>
        <Heading level="2" size="large" className={styles.header_wrapper}>
          Oversikt over avtaler
        </Heading>
        <Avtalefilter />
        <AvtaleTabell />
      </ContainerLayoutOversikt>
    </MainContainer>
  );
}
