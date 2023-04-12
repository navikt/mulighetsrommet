import { Heading } from "@navikt/ds-react";
import { Tiltaksgjennomforingfilter } from "../../components/filter/Tiltaksgjennomforingfilter";
import { TiltaksgjennomforingOversikt } from "../../components/tiltaksgjennomforinger/TiltaksgjennomforingOversikt";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import styles from "../Page.module.scss";

export function TiltaksgjennomforingerPage() {
  return (
    <MainContainer>
      <ContainerLayout>
        <Heading level="2" size="large" className={styles.header_wrapper}>
          Oversikt over tiltaksgjennomføringer
        </Heading>
        <Tiltaksgjennomforingfilter />
        <TiltaksgjennomforingOversikt />
      </ContainerLayout>
    </MainContainer>
  );
}
