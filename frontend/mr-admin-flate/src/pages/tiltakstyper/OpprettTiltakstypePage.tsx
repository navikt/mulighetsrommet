import {
  BodyLong,
  Button,
  Checkbox,
  Heading,
  Select,
  Switch,
  TextField,
  UNSAFE_DatePicker,
  UNSAFE_useRangeDatepicker,
} from "@navikt/ds-react";
import { FieldHookConfig, Form, Formik, useField, useFormik } from "formik";
import { z } from "zod";
import { toFormikValidationSchema } from "zod-formik-adapter";
import styles from "../tiltaksgjennomforinger/Oversikt.module.scss";
import formStyles from "./OpprettTiltakstypePage.module.scss";

// TODO Se på boolean-verdier i zod og Formik
// TODO Skal vi ha auto-lagring av skjema?

const Schema = z.object({
  tiltakstypenavn: z.string({ required_error: "Tiltakstypen må ha et navn" }),
  tiltaksgruppekode: z.string({
    required_error: "Tiltaksgruppekode må være satt",
  }),
  tiltakskode: z.string({ required_error: "Tiltakskode må være satt" }),
  rettTilTiltakspenger: z.string().array(),
  administrasjonskode: z.string({
    required_error: "Du må sette en administrasjonskode",
  }),
  kopiAvTilsagnsbrev: z.string().array(),
  arkivkode: z.string({ required_error: "Du må sette en arkivkode" }),
  harAnskaffelse: z.string().array(),
  rammeavtale: z.string({ required_error: "Du må velge en rammeavtale" }),
  opplaringsgruppe: z.string({
    required_error: "Du må velge en opplæringsgruppe",
  }),
  handlingsplan: z.string({ required_error: "Du må velge en handlingsplan" }),
  harObligatoriskSluttdato: z.string().array(),
  varighet: z.string({ required_error: "Du må sette en varighet" }),
  harStatusSluttdato: z.string().array(),
  harStatusMeldeplikt: z.string().array(),
  harStatusVedtak: z.string().array(),
  harStatusIAAvtale: z.string().array(),
  harStatusTilleggstonad: z.string().array(),
  harStatusUtdanning: z.string().array(),
  harAutomatiskTilsagsnbrev: z.string().array(),
  harStatusBegrunnelseInnsok: z.string().array(),
  harStatusHenvisningsbrev: z.string().array(),
  harStatusKopibrev: z.string().array(),
});

type SchemaValues = z.infer<typeof Schema>;

function Tekstfelt({
  label,
  name,
  hjelpetekst,
  ...props
}: {
  name: keyof SchemaValues;
  label: string;
  hjelpetekst?: string;
} & FieldHookConfig<any>) {
  const [field, meta] = useField({ name, ...props });
  return (
    <TextField
      description={hjelpetekst}
      size="small"
      label={label}
      {...field}
      error={meta.touched && meta.error}
    />
  );
}

function SelectFelt({
  label,
  name,
  defaultBlank = true,
  defaultBlankName = "",
  ...props
}: {
  name: keyof SchemaValues;
  label: string;
  defaultBlank?: boolean;
  defaultBlankName?: string;
} & FieldHookConfig<any>) {
  const [field, meta] = useField({ name, ...props });
  return (
    <Select
      size="small"
      label={label}
      {...field}
      error={meta.touched && meta.error}
    >
      {defaultBlank ? <option value="">{defaultBlankName}</option> : null}
      {props.children}
    </Select>
  );
}

function SwitchFelt({
  name,
  ...props
}: { name: keyof SchemaValues } & FieldHookConfig<any>) {
  const [field] = useField({ name, ...props });
  return (
    <Switch size="small" {...field} defaultChecked={field.value}>
      {props.children}
    </Switch>
  );
}

function CheckboxFelt(
  props: { name: keyof SchemaValues } & FieldHookConfig<any>
) {
  const [field] = useField({ ...props, type: "checkbox" });

  return <Checkbox {...field}>{props.children}</Checkbox>;
}

export function OpprettTiltakstype() {
  const { datepickerProps, toInputProps, fromInputProps, selectedRange } =
    UNSAFE_useRangeDatepicker({
      fromDate: new Date("Aug 23 2019"),
      onRangeChange: console.log,
    });
  const initialValues: SchemaValues = {
    tiltakstypenavn: "",
    tiltaksgruppekode: "",
    tiltakskode: "",
    rettTilTiltakspenger: [],
    administrasjonskode: "",
    kopiAvTilsagnsbrev: [],
    arkivkode: "",
    harAnskaffelse: [],
    rammeavtale: "",
    opplaringsgruppe: "",
    handlingsplan: "",
    harObligatoriskSluttdato: [],
    varighet: "",
    harStatusSluttdato: [],
    harStatusMeldeplikt: [],
    harStatusVedtak: [],
    harStatusIAAvtale: [],
    harStatusTilleggstonad: [],
    harStatusUtdanning: [],
    harAutomatiskTilsagsnbrev: [],
    harStatusBegrunnelseInnsok: [],
    harStatusHenvisningsbrev: [],
    harStatusKopibrev: [],
  };
  const tiltaksgruppekoder = ["OPPFOLG", "FOLKHOY"]; // TODO Disse bør komme fra et API så vi kan prepopulere en select-component
  const rammeavtaler = [
    { id: 1, navn: "Den beste rammeavtalen" },
    { id: 2, navn: "Den nest beste rammeavtalen" },
  ]; // TODO Rammeavtaler bør komme fra API
  const opplaringsgrupper = [
    { id: 1, navn: "Den beste opplæringsgruppen" },
    { id: 2, navn: "Den neste beste opplæringsgruppen" },
  ]; // TODO Skal dette komme fra et api?
  const handlingsplaner = [
    {
      // TODO Skal dette komme fra et API?
      id: 1,
      navn: "obligatorisk sluttdato",
    },
  ];

  const onSave = () => {
    console.log("Lagrer...");
  };

  return (
    <>
      <Heading className={styles.overskrift} size="large">
        Opprett ny tiltakstype
      </Heading>
      <BodyLong className={styles.body} size="small">
        Her kan du opprette eller redigere en tiltakstype
      </BodyLong>
      <Formik<SchemaValues>
        initialValues={initialValues}
        validationSchema={toFormikValidationSchema(Schema)}
        onSubmit={(values, actions) => {
          alert(JSON.stringify(values, null, 2));
          actions.setSubmitting(false);
        }}
      >
        {({ handleSubmit }) => (
          <Form
            className={formStyles.form}
            onSubmit={(e) => e.preventDefault()}
          >
            <Tekstfelt name="tiltakstypenavn" label="Navn på tiltakstype" />
            <SelectFelt name="tiltaksgruppekode" label="Tiltaksgruppekode">
              {tiltaksgruppekoder.map((kode) => (
                <option key={kode} value={kode}>
                  {kode}
                </option>
              ))}
            </SelectFelt>
            <Tekstfelt name="tiltakskode" label="Tiltakskode" />
            <UNSAFE_DatePicker {...datepickerProps}>
              <div style={{ display: "flex", gap: "5rem" }}>
                <UNSAFE_DatePicker.Input {...fromInputProps} label="Fra" />
                <UNSAFE_DatePicker.Input {...toInputProps} label="Til" />
              </div>
            </UNSAFE_DatePicker>
            <CheckboxFelt
              name="rettTilTiltakspenger"
              value={"rettTilTiltakspenger"}
            >
              Rett på tiltakspenger
            </CheckboxFelt>
            <Tekstfelt name="administrasjonskode" label="Administrasjonskode" />
            <CheckboxFelt name="kopiAvTilsagnsbrev" value="kopiAvTilsagnsbrev">
              Kopi av tilsagnsbrev
            </CheckboxFelt>
            <Tekstfelt name="arkivkode" label="Arkivkode" />
            <CheckboxFelt name="harAnskaffelse" value="harAnskaffelse">
              Anskaffelse
            </CheckboxFelt>
            <SelectFelt name="rammeavtale" label="Rammeavtale">
              {rammeavtaler.map(({ id, navn }) => (
                <option key={id} value={id}>
                  {navn}
                </option>
              ))}
            </SelectFelt>
            <SelectFelt
              name="opplaringsgruppe"
              label="Opplæringsgruppe"
              defaultBlankName="Ingen opplæringsgruppe"
            >
              {opplaringsgrupper.map(({ id, navn }) => (
                <option key={id} value={id}>
                  {navn}
                </option>
              ))}
            </SelectFelt>
            <SelectFelt name="handlingsplan" label="Handlingsplan">
              {handlingsplaner.map(({ id, navn }) => (
                <option key={id} value={id}>
                  {navn}
                </option>
              ))}
            </SelectFelt>
            <CheckboxFelt
              name="harObligatoriskSluttdato"
              value="harObligatoriskSluttdato"
            >
              Obligatorisk sluttdato
            </CheckboxFelt>
            <Tekstfelt
              name="varighet"
              label="Varighet"
              hjelpetekst="Maks antall måneder"
            />

            <CheckboxFelt name="harStatusSluttdato" value="harStatusSluttdato">
              Status sluttdato
            </CheckboxFelt>
            <CheckboxFelt
              name="harStatusMeldeplikt"
              value="harStatusMeldeplikt"
            >
              Status meldeplinkt
            </CheckboxFelt>
            <CheckboxFelt name="harStatusVedtak" value="harStatusVedtak">
              Status vedtak
            </CheckboxFelt>
            <CheckboxFelt name="harStatusIAAvtale" value="harStatusIAAvtale">
              Status IA Avtale
            </CheckboxFelt>
            <CheckboxFelt
              name="harStatusTilleggstonad"
              value="harStatusTilleggstonad"
            >
              Status tilleggsstønad
            </CheckboxFelt>
            <CheckboxFelt name="harStatusUtdanning" value="harStatusUtdanning">
              Status utdanning
            </CheckboxFelt>

            <CheckboxFelt
              name="harAutomatiskTilsagsnbrev"
              value="harAutomatiskTilsagsnbrev"
            >
              Automatisk tilsagnsbrev
            </CheckboxFelt>
            <CheckboxFelt
              name="harStatusBegrunnelseInnsok"
              value="harStatusBegrunnelseInnsok"
            >
              Status begrunnelse innsøk
            </CheckboxFelt>
            <CheckboxFelt
              name="harStatusHenvisningsbrev"
              value="harStatusHenvisningsbrev"
            >
              Status henvisningsbrev
            </CheckboxFelt>
            <CheckboxFelt name="harStatusKopibrev" value="harStatusKopibrev">
              Status kopibrev
            </CheckboxFelt>
            <div className={formStyles.separator} />
            <div className={formStyles.summaryContainer}>
              <div>
                <span>Sist oppdatert: TBA</span>{" "}
                {/** TODO Her må sist lagret inn */}
              </div>
              <div style={{ display: "flex", gap: "1rem" }}>
                <Button onClick={onSave} variant="tertiary">
                  Lagre
                </Button>
                <Button onClick={() => handleSubmit()}>Publiser</Button>
              </div>
            </div>
          </Form>
        )}
      </Formik>
    </>
  );
}
