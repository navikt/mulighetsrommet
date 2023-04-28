import { Heading } from "@navikt/ds-react";
import { Tiltaksgjennomforingfilter } from "../../components/filter/Tiltaksgjennomforingfilter";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import styles from "../Page.module.scss";
import { TiltaksgjennomforingsTabell } from "../../components/tabell/TiltaksgjennomforingsTabell";

export function TiltaksgjennomforingerPage() {
  return (
    <MainContainer>
      <ContainerLayout>
        <Heading level="2" size="large" className={styles.header_wrapper}>
          Oversikt over tiltaksgjennomføringer
        </Heading>
        <Tiltaksgjennomforingfilter />
        <TiltaksgjennomforingsTabell />
      </ContainerLayout>
    </MainContainer>
  );
}
