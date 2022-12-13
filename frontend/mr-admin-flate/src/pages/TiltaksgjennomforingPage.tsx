import { Alert, Heading } from "@navikt/ds-react";
import { useTiltaksgjennomforingById } from "../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import Tilbakeknapp from "mulighetsrommet-veileder-flate/src/components/tilbakeknapp/Tilbakeknapp";

interface TiltaksgjennomforingPageProps {
  fagansvarlig?: boolean;
}

export function TiltaksgjennomforingPage({
  fagansvarlig = false,
}: TiltaksgjennomforingPageProps) {
  const optionalTiltaksgjennomforing = useTiltaksgjennomforingById();

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
    <div>
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
      <Heading size="medium" level="1">
        {tiltaksgjennomforing.tiltaksnummer} - {tiltaksgjennomforing.navn}
      </Heading>
      <dl>
        <dt>Tiltakstype:</dt>
        {/**
         * TODO Bytte ut med navn
         */}
        <dd>{tiltaksgjennomforing.tiltakstypeId}</dd>
        <dt>Virksomhetsnummer</dt>
        <dd>{tiltaksgjennomforing.virksomhetsnummer}</dd>
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
