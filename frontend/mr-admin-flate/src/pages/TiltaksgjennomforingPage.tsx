import { Alert, Heading } from "@navikt/ds-react";
import { useTiltaksgjennomforingById } from "../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import Tilbakeknapp from "mulighetsrommet-veileder-flate/src/components/tilbakeknapp/Tilbakeknapp";
import { formaterDato } from "../utils/Utils";
import styles from "./TiltaksgjennomforingPage.module.scss";

interface TiltaksgjennomforingPageProps {
  fagansvarlig?: boolean;
}

export function TiltaksgjennomforingPage({
  fagansvarlig = false,
}: TiltaksgjennomforingPageProps) {
  const optionalTiltaksgjennomforing = useTiltaksgjennomforingById();

  console.log({ optionalTiltaksgjennomforing });

  if (optionalTiltaksgjennomforing.isFetching) {
    return null;
  }

  if (!optionalTiltaksgjennomforing.data) {
    return (
      <Alert variant="warning">Klarte ikke finne tiltaksgjennomføring</Alert>
    );
  }

  const tiltaksgjennomforing = optionalTiltaksgjennomforing.data;
  return (
    <div className={styles.container}>
      <Tilbakeknapp
        tilbakelenke={
          fagansvarlig
            ? `/oversikt/${tiltaksgjennomforing.tiltakstypeId}`
            : "/oversikt"
        }
        tekst={
          fagansvarlig ? "Tilbake til tiltakstype" : "Tilbake til oversikt"
        }
      />
      <Heading size="large" level="1">
        {tiltaksgjennomforing.tiltaksnummer} - {tiltaksgjennomforing.navn}
      </Heading>
      <p>
        Tiltaksgjennomføringen har startdato:{" "}
        {formaterDato(tiltaksgjennomforing.fraDato)} og sluttdato{" "}
        {formaterDato(tiltaksgjennomforing.tilDato)}
      </p>
      <dl>
        <dt>Tiltaksnummer</dt>
        <dd>{tiltaksgjennomforing.tiltaksnummer}</dd>
        <dt>Tiltakstype</dt>
        <dd>{tiltaksgjennomforing.tiltakstypeNavn}</dd>
        <dt>Kode for tiltakstype:</dt>
        <dd>{tiltaksgjennomforing.tiltakskode}</dd>
        <dt>Virksomhetsnummer</dt>
        <dd>{tiltaksgjennomforing.virksomhetsnummer}</dd>
        <dt>Startdato</dt>
        <dd>{formaterDato(tiltaksgjennomforing.fraDato)} </dd>
        <dt>Sluttdato</dt>
        <dd>{formaterDato(tiltaksgjennomforing.tilDato)} </dd>
      </dl>

      {/**
       * TODO Implementere skjema for opprettelse av tiltaksgjennomføring
       */}
      {/* <p>Her kan du opprette en gjennomføring</p>
      <Formik<Values>
        initialValues={{
          tiltakgjennomforingId: "",
          sakId: "",
        }}
        validationSchema={toFormikValidationSchema(Schema)}
        onSubmit={(values, actions) => {
          setTimeout(() => {
            alert(JSON.stringify(values, null, 2));
            actions.setSubmitting(false);
          }, 1000);
        }}
      >
        {() => (
          <Form>
            <Tekstfelt
              name="tiltakgjennomforingId"
              type="text"
              label="ID for tiltaksgjennomføring"
            />
            <Tekstfelt name="sakId" type="text" label="ID for sak" />
            <button type="submit">Opprett</button>
          </Form>
        )}
      </Formik> */}
    </div>
  );
}
