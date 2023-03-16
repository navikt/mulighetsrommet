import { Button, ReadMore } from "@navikt/ds-react";
import classNames from "classnames";
import { Form, Formik, FormikHelpers } from "formik";
import { ReactNode } from "react";
import z from "zod";
import { toFormikValidationSchema } from "zod-formik-adapter";
import {
  Datovelger,
  SelectFelt,
  TekstareaFelt,
  Tekstfelt,
} from "../../skjema/OpprettComponents";
import styles from "./OpprettAvtaleContainer.module.scss";

const Schema = z.object({
  avtalenavn: z.string({ required_error: "En avtale må ha et navn" }),
  fraDato: z.string({ required_error: "En avtale må ha en startdato" }),
  tilDato: z.string({ required_error: "En avtale må ha en sluttdato" }),
  tiltakstype: z.string({ required_error: "Du må velge en tiltakstype" }),
  enhet: z.string({ required_error: "Du må velge en enhet" }),
  antallPlasser: z.number({ required_error: "Du må sette antall plasser" }),
  leverandor: z.string({
    required_error: "Du må velge en leverandør for avtalen",
  }),
  avtaletype: z.string({ required_error: "Du må velge en avtaletype" }),
  prisOgBetalingsbetingelser: z.string({
    required_error: "Du må skrive inn pris og betalingsbetingelser",
  }),
  avtaleansvarlig: z.string({
    required_error: "Du må velge en avtaleansvarlig",
  }),
});

type inferredSchema = z.infer<typeof Schema>;

export function OpprettAvtaleContainer() {
  return (
    <div
      style={{
        width: "800px",
        margin: "0 auto",
        background: "white",
        padding: "1rem",
      }}
    >
      <Formik
        initialValues={{
          avtalenavn: "",
          fraDato: "",
          tilDato: "",
          tiltakstype: "",
          enhet: "",
          antallPlasser: 0,
          leverandor: "",
          avtaletype: "",
          prisOgBetalingsbetingelser: "",
          avtaleansvarlig: "",
        }}
        onSubmit={(
          values: inferredSchema,
          { setSubmitting }: FormikHelpers<inferredSchema>
        ) => {
          setTimeout(() => {
            alert(JSON.stringify(values, null, 2));
            setSubmitting(false);
          }, 500);
        }}
        validationSchema={toFormikValidationSchema(Schema)}
      >
        <Form className={styles.form_container}>
          <FormGroup cols={1}>
            <Tekstfelt<inferredSchema> name="avtalenavn" label="Avtalenavn" />
          </FormGroup>
          <FormGroup cols={1}>
            <Datovelger<inferredSchema>
              fra={{ name: "fraDato", label: "Start" }}
              til={{ name: "tilDato", label: "Slutt" }}
            />
          </FormGroup>
          <Button
            variant="secondary"
            onClick={() => alert("Forleng avtalen er ikke implementert enda")}
            style={{ marginBottom: "1rem" }}
          >
            Forleng avtalen
          </Button>
          <ReadMore header="Når og hvor mye kan jeg forlenge?">
            Lorem ipsum dolor sit, amet consectetur adipisicing elit.
            Laudantium, voluptas nostrum provident veritatis corporis laboriosam
            iure ipsum! Harum dolor aperiam provident alias quo sit repellat.
            Consequuntur quas commodi iste. Odit.
          </ReadMore>
          <FormGroup cols={2}>
            <SelectFelt<inferredSchema> name="tiltakstype" label="Tiltakstype">
              <option value="oppfolging">Oppfølging</option>
              <option value="arr">Arbeidsrettet rehabilitering</option>
            </SelectFelt>
            <SelectFelt<inferredSchema> name="enhet" label="Enhet">
              <option value="1234">NAV Ringsaker</option>
              <option value="5678">NAV Bergen</option>
            </SelectFelt>
            <Tekstfelt<inferredSchema>
              name="antallPlasser"
              label="Antall plasser"
              type="text"
            />
            <SelectFelt<inferredSchema> name="leverandor" label="Leverandør">
              <option value="1234">Joblearn AS</option>
            </SelectFelt>
            <SelectFelt<inferredSchema> name="avtaletype" label="Avtaletype">
              <option value="forgod">Forhåndsgodkjent avtale</option>
            </SelectFelt>
          </FormGroup>
          <FormGroup cols={1}>
            <TekstareaFelt<inferredSchema>
              name="prisOgBetalingsbetingelser"
              label="Pris og betalingsbetingelser"
              size="medium"
            />
          </FormGroup>
          <FormGroup cols={2}>
            <SelectFelt<inferredSchema>
              name="avtaleansvarlig"
              label="Avtaleansvarlig"
            >
              <option value="m165757">M165757</option>
            </SelectFelt>
          </FormGroup>
          <div className={styles.content_right}>
            <Button type="submit">Registrer avtale</Button>
          </div>
        </Form>
      </Formik>
    </div>
  );
}

function FormGroup({ children, cols }: { children: ReactNode; cols: number }) {
  return (
    <div
      className={classNames(styles.form_group, styles.grid, {
        [styles.grid_1]: cols === 1,
        [styles.grid_2]: cols === 2,
      })}
    >
      {children}
    </div>
  );
}
