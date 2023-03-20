import { Heading } from "@navikt/ds-react";
import { Avtalefilter } from "../../components/filter/Avtalefilter";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import { NavigeringHeader } from "../forside/NavigeringHeader";
import styles from "../Page.module.scss";
import { AvtaleOversikt } from "../../components/avtaler/AvtaleOversikt";

export function AvtalerPage() {
  return (
    <MainContainer>
      <NavigeringHeader />
      <ContainerLayout>
        <Heading level="2" size="large" className={styles.header_wrapper}>
          Oversikt over avtaler
        </Heading>
        <Avtalefilter />
        <AvtaleOversikt />
      </ContainerLayout>
    </MainContainer>
  );
}
