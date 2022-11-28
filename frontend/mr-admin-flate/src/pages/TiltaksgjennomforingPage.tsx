import { Alert, Tag, TextField } from "@navikt/ds-react";
import { Form, Formik, useField } from "formik";
import { Link } from "react-router-dom";
import { z } from "zod";
import { toFormikValidationSchema } from "zod-formik-adapter";
import { useTiltaksgjennomforingById } from "../api/tiltaksgjennomforing/useTiltaksgjennomforingById";

const Schema = z.object({
  tiltakgjennomforingId: z.string({
    required_error: "ID for tiltaksgjennføring må settes",
  }),
  sakId: z.string({
    required_error: "ID for sak må settes",
  }),
});

type Values = z.infer<typeof Schema>;

const Tekstfelt = ({
  label,
  ...props
}: {
  label: string;
  name: keyof Values;
  type: "text";
}) => {
  const [field, meta] = useField(props);
  return (
    <div
      style={{ display: "flex", flexDirection: "column", marginBottom: "1rem" }}
    >
      <TextField
        size="small"
        label={label}
        {...field}
        {...props}
        error={meta.touched && meta.error ? meta.error : null}
      />
    </div>
  );
};

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
