import { Button, ReadMore, Select, TextField } from "@navikt/ds-react";
import classNames from "classnames";
import { Form, Formik, FormikHelpers } from "formik";
import { ReactNode, useState } from "react";
import z from "zod";
import { toFormikValidationSchema } from "zod-formik-adapter";
import { useForm, SubmitHandler, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Datovelger,
  SelectFelt,
  TekstareaFelt,
  Tekstfelt,
} from "../../skjema/OpprettComponents";
import styles from "./OpprettAvtaleContainer.module.scss";

const Schema = z.object({
  avtalenavn: z
    .string({ required_error: "En avtale må ha et navn" })
    .min(5, "Et avtalenavn må minst være 5 tegn langt"),
  tiltakstype: z.string({ required_error: "Du må velge en tiltakstype" }),
  avtaletype: z.string({ required_error: "Du må velge en avtaletype" }),
  leverandor: z.string({
    required_error: "Du må velge en leverandør for avtalen",
  }),
  enhet: z.string({ required_error: "Du må velge en enhet" }),
  antallPlasser: z.number({ required_error: "Du må sette antall plasser" }),
  fraDato: z.string({ required_error: "En avtale må ha en startdato" }),
  tilDato: z.string({ required_error: "En avtale må ha en sluttdato" }),
});

/*const Schema = z.object({
  avtalenavn: z
    .string({ required_error: "En avtale må ha et navn" })
    .min(5, "Et avtalenavn må minst være 5 tegn langt"),
  tiltakstype: z.string({ required_error: "Du må velge en tiltakstype" }),
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
});*/

export type inferredSchema = z.infer<typeof Schema>;

export function OpprettAvtaleContainer() {
  const [type, setType] = useState<"formik" | "react-hook-form">(
    "react-hook-form"
  );

  const handleChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
    setType(event.currentTarget.value as any);
  };

  return (
    <div
      style={{
        width: "800px",
        margin: "0 auto",
        background: "white",
        padding: "1rem",
      }}
    >
      <select value={type} onChange={handleChange}>
        <option value="formik">Formik</option>
        <option value="react-hook-form">React-hook-form</option>
      </select>
      {type === "formik" ? <FormikContainer /> : <ReactHookFormContainer />}
    </div>
  );
}

function ReactHookFormContainer() {
  /*const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<inferredSchema>({
    resolver: zodResolver(Schema),
  });*/
  const form = useForm<inferredSchema>({
    resolver: zodResolver(Schema),
  });
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = form;
  const onSubmit: SubmitHandler<inferredSchema> = (data) => console.log(data);
  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(onSubmit)}>
        <FormGroup>
          <TextField
            error={errors.avtalenavn?.message}
            label="Avtalenavn"
            {...register("avtalenavn")}
          />
        </FormGroup>
        <FormGroup cols={1}>
          <Datovelger
            fra={{
              label: "Start",
              error: errors.fraDato?.message,
              ...register("fraDato"),
            }}
            til={{
              label: "Slutt",
              error: errors.tilDato?.message,
              ...register("tilDato"),
            }}
          />
        </FormGroup>
        <FormGroup cols={2}>
          <Select label={"Tiltakstype"} {...register("tiltakstype")}>
            <option value="oppfolging">Oppfølging</option>
            <option value="arr">Arbeidsrettet rehabilitering</option>
          </Select>
          <TextField
            error={errors.enhet?.message}
            label="Enhet"
            {...register("enhet")}
          />
          <TextField
            error={errors.antallPlasser?.message}
            label="Antall plasser"
            {...register("antallPlasser", { valueAsNumber: true })}
          />
          <Select label={"Leverandør"} {...register("leverandor")}>
            <option value="fretty">Fretex jobb og utvikling</option>
          </Select>
          <Select label={"Avtaletype"} {...register("avtaletype")}>
            <option value="forhandsgodkjent">Forhåndsgodkjent avtale</option>
          </Select>
        </FormGroup>
        <div className={styles.content_right}>
          <Button type="submit">Registrer avtale</Button>
        </div>
      </form>
    </FormProvider>
  );
}

function FormikContainer() {
  return (
    <>Hei</>
    /*<Formik
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
          Lorem ipsum dolor sit, amet consectetur adipisicing elit. Laudantium,
          voluptas nostrum provident veritatis corporis laboriosam iure ipsum!
          Harum dolor aperiam provident alias quo sit repellat. Consequuntur
          quas commodi iste. Odit.
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
    </Formik>*/
  );
}

function FormGroup({
  children,
  cols = 1,
}: {
  children: ReactNode;
  cols?: number;
}) {
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
