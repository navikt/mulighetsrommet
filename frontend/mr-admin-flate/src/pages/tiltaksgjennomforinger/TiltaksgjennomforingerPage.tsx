import { Heading } from "@navikt/ds-react";
import { Tiltaksgjennomforingfilter } from "../../components/filter/Tiltaksgjennomforingfilter";
import { ContainerLayoutOversikt } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import styles from "../Page.module.scss";
import { TiltaksgjennomforingsTabell } from "../../components/tabell/TiltaksgjennomforingsTabell";

export function TiltaksgjennomforingerPage() {
  return (
    <MainContainer>
      <ContainerLayoutOversikt>
        <Heading level="2" size="large" className={styles.header_wrapper}>
          Oversikt over tiltaksgjennomføringer
        </Heading>
        <Tiltaksgjennomforingfilter />
        <TiltaksgjennomforingsTabell />
      </ContainerLayoutOversikt>
    </MainContainer>
  );
}
