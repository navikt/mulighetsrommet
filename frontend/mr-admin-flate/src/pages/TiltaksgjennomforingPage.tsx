import { TextField } from "@navikt/ds-react";
import { Form, Formik, useField } from "formik";
import { z } from "zod";
import { toFormikValidationSchema } from "zod-formik-adapter";

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
      <TextField size="small" label={label} {...field} {...props} />
      {meta.touched && meta.error ? (
        <div className="error">{meta.error}</div>
      ) : null}
    </div>
  );
};

export function TiltaksgjennomforingPage() {
  return (
    <div>
      <h1>Opprett tiltaksgjennomføring</h1>
      <p>Her kan du opprette en gjennomføring</p>
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
            <button type="submit">Submit</button>
          </Form>
        )}
      </Formik>
    </div>
  );
}
