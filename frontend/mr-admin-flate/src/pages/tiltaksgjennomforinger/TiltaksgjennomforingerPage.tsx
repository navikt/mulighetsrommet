import { Heading } from "@navikt/ds-react";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import { NavigeringHeader } from "../forside/NavigeringHeader";
import styles from "../Page.module.scss";
import { Tiltaksgjennomforingfilter } from "../../components/filter/Tiltaksgjennomforingfilter";
import { TiltaksgjennomforingOversikt } from "../../components/tiltaksgjennomforinger/TiltaksgjennomforingOversikt";

export function TiltaksgjennomforingerPage() {
  return (
    <MainContainer>
      <NavigeringHeader />
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
