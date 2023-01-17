import { BodyLong, Button, Heading } from "@navikt/ds-react";
import { Form, Formik } from "formik";
import { Link } from "react-router-dom";

import { toFormikValidationSchema } from "zod-formik-adapter";
import styles from "../../tiltaksgjennomforinger/Oversikt.module.scss";
import formStyles from "./OpprettTiltaksgjennomforingPage.module.scss";
import { OpprettTiltaksgjennomforingSchema } from "./OpprettTiltaksgjennomforingSchemaValidation";
import {
  Datovelger,
  OptionalSchemaValues,
  Tekstfelt,
} from "../../OpprettComponents";

export function OpprettTiltaksgjennomforing() {
  const initialValues: OptionalSchemaValues = {
    tiltaksgjennomforingnavn: undefined,
    fraDato: undefined,
    tilDato: undefined,
  };

  return (
    <>
      <Link style={{ marginBottom: "1rem", display: "block" }} to="/oversikt">
        Tilbake til oversikt
      </Link>
      <Heading className={styles.overskrift} size="large">
        Opprett ny tiltaksgjennomforing
      </Heading>
      <BodyLong className={styles.body} size="small">
        Her kan du opprette eller redigere en tiltaksgjennomforing
      </BodyLong>
      <Formik<OptionalSchemaValues>
        initialValues={initialValues}
        validationSchema={toFormikValidationSchema(
          OpprettTiltaksgjennomforingSchema
        )}
        onSubmit={(values, actions) => {
          // TODO Må sende data til backend
          console.log(values);
          alert(JSON.stringify(values, null, 2));
          actions.setSubmitting(false);
        }}
      >
        {({ handleSubmit }) => (
          <>
            <Form
              className={formStyles.form}
              onSubmit={(e) => e.preventDefault()}
            >
              <Tekstfelt
                name="tiltaksgjennomforingnavn"
                label="Navn på tiltaksgjennomforing"
              />
              <Datovelger />
              <div className={formStyles.separator} />
              <div className={formStyles.summaryContainer}>
                <div style={{ display: "flex", gap: "1rem" }}>
                  <Button onClick={() => handleSubmit()}>Publiser</Button>
                </div>
              </div>
            </Form>
          </>
        )}
      </Formik>
    </>
  );
}
