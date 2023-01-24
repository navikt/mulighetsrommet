import { BodyLong, Button, Heading } from "@navikt/ds-react";
import { Form, Formik } from "formik";
import { Link } from "react-router-dom";

import { toFormikValidationSchema } from "zod-formik-adapter";
import {
  CheckboxFelt as CheckboxFeltComponent,
  Datovelger,
  OpprettTiltaksgruppeSchemaValues, OpprettTiltakstypeSchemaValues,
  OptionalTiltaksgjennomforingSchemaValues,
  Tekstfelt as TekstfeltComponent
} from "../../OpprettComponents";
import styles from "../../Oversikt.module.scss";
import formStyles from "./OpprettTiltaksgruppePage.module.scss";
import { OpprettTiltaksgruppeSchema } from "./OpprettTiltaksgruppeSchemaValidation";

const Tekstfelt = TekstfeltComponent<OpprettTiltaksgruppeSchemaValues>;
const CheckboxFelt = CheckboxFeltComponent<OpprettTiltakstypeSchemaValues>;

export function OpprettTiltaksgruppe() {
  const initialValues: OptionalTiltaksgjennomforingSchemaValues = {
    tiltaksgjennomforingnavn: undefined,
    fraDato: undefined,
    tilDato: undefined,
    harAutomatiskTilsagnsbrev: false,
    harStatusBegrunnelseInnsok: false,
    harStatusHenvisningsbrev: false,
    harStatusKopibrev: false,
  };

  return (
    <>
      <Link
        style={{ marginBottom: "1rem", display: "block" }}
        to="/tiltaksgrupper"
      >
        Tilbake
      </Link>
      <Heading className={styles.overskrift} size="large">
        Opprett ny tiltaksgruppe
      </Heading>
      <BodyLong className={styles.body} size="small">
        Her kan du opprette eller redigere en tiltaksgruppe
      </BodyLong>
      <Formik<OptionalTiltaksgjennomforingSchemaValues>
        initialValues={initialValues}
        validationSchema={toFormikValidationSchema(OpprettTiltaksgruppeSchema)}
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
              <Tekstfelt name="tiltaksgruppenavn" label="Tiltaksgruppenavn" />
              <Tekstfelt name="tiltaksgruppekode" label="Tiltaksgruppekode" />
              <Datovelger />
              <CheckboxFelt name="harAutomatiskTilsagnsbrev">
                Automatisk tilsagnsbrev
              </CheckboxFelt>
              <CheckboxFelt name="harStatusBegrunnelseInnsok">
                Status begrunnelse innsøk
              </CheckboxFelt>
              <CheckboxFelt name="harStatusHenvisningsbrev">
                Status henvisningsbrev
              </CheckboxFelt>
              <CheckboxFelt name="harStatusKopibrev">
                Status kopibrev
              </CheckboxFelt>
              <div className={formStyles.separator} />
              <div className={formStyles.summaryContainer}>
                <div style={{ display: "flex", gap: "1rem" }}>
                  <Button type="submit" onClick={() => handleSubmit()}>
                    Publiser
                  </Button>
                </div>
              </div>
            </Form>
          </>
        )}
      </Formik>
    </>
  );
}
