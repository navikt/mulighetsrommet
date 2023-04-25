import { Heading } from "@navikt/ds-react";
import { Avtalefilter } from "../../components/filter/Avtalefilter";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import styles from "../Page.module.scss";
import { AvtaleTabell } from "../../components/tabell/AvtaleTabell";

export function AvtalerPage() {
  return (
    <MainContainer>
      <ContainerLayout>
        <Heading level="2" size="large" className={styles.header_wrapper}>
          Oversikt over avtaler
        </Heading>
        <Avtalefilter />
        <AvtaleTabell />
      </ContainerLayout>
    </MainContainer>
  );
}
