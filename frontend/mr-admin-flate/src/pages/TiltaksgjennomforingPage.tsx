import { Alert, Tag } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { useTiltaksgjennomforingById } from "../api/tiltaksgjennomforing/useTiltaksgjennomforingById";

export function TiltaksgjennomforingPage() {
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
      <Link to="/oversikt">Tilbake til oversikt</Link>
      <p>
        <Tag variant="info">{tiltaksgjennomforing.tilgjengelighet}</Tag>
      </p>
      <h1>
        {tiltaksgjennomforing.tiltaksnummer} - {tiltaksgjennomforing.navn}
      </h1>
      <dl>
        <dt>År:</dt>
        <dd>{tiltaksgjennomforing.aar}</dd>
        <dt>Tiltakskode</dt>
        <dd>{tiltaksgjennomforing.tiltakskode}</dd>
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
