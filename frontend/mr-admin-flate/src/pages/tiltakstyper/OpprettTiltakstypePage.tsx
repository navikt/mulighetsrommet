import {
  BodyLong,
  Button,
  Checkbox,
  Heading,
  Select,
  TextField,
  UNSAFE_DatePicker,
  UNSAFE_useRangeDatepicker,
} from "@navikt/ds-react";
import { FieldHookConfig, Form, Formik, useField } from "formik";
import { Link } from "react-router-dom";
import { z } from "zod";
import { toFormikValidationSchema } from "zod-formik-adapter";
import styles from "../tiltaksgjennomforinger/Oversikt.module.scss";
import formStyles from "./OpprettTiltakstypePage.module.scss";

// TODO Skal vi ha auto-lagring av skjema?

// TODO Dra skjema-validering ut i egen fil?
const BooleanDefaultFalse = z.boolean().default(false);
const Schema = z.object({
  tiltakstypenavn: z.string({ required_error: "Tiltakstypen må ha et navn" }),
  fraDato: z.date({
    required_error: "Du må sette en fra-dato for tiltakstypen",
  }),
  tilDato: z.date({
    required_error: "Du må sette en til-dato for tiltakstypen",
  }),
  tiltaksgruppekode: z.string({
    required_error: "Tiltaksgruppekode må være satt",
  }),
  tiltakskode: z.string({ required_error: "Tiltakskode må være satt" }),
  rettTilTiltakspenger: BooleanDefaultFalse,
  administrasjonskode: z.string({
    required_error: "Du må sette en administrasjonskode",
  }),
  kopiAvTilsagnsbrev: BooleanDefaultFalse,
  arkivkode: z.string({ required_error: "Du må sette en arkivkode" }),
  harAnskaffelse: BooleanDefaultFalse,
  rammeavtale: z.string({ required_error: "Du må velge en rammeavtale" }),
  opplaringsgruppe: z.string({
    required_error: "Du må velge en opplæringsgruppe",
  }),
  handlingsplan: z.string({ required_error: "Du må velge en handlingsplan" }),
  harObligatoriskSluttdato: BooleanDefaultFalse,
  varighet: z.string({ required_error: "Du må sette en varighet" }),
  harStatusSluttdato: BooleanDefaultFalse,
  harStatusMeldeplikt: BooleanDefaultFalse,
  harStatusVedtak: BooleanDefaultFalse,
  harStatusIAAvtale: BooleanDefaultFalse,
  harStatusTilleggstonad: BooleanDefaultFalse,
  harStatusUtdanning: BooleanDefaultFalse,
  harAutomatiskTilsagnsbrev: BooleanDefaultFalse,
  harStatusBegrunnelseInnsok: BooleanDefaultFalse,
  harStatusHenvisningsbrev: BooleanDefaultFalse,
  harStatusKopibrev: BooleanDefaultFalse,
});

type SchemaValues = z.infer<typeof Schema>;

// TODO Dra komponenter ut i egen fil?
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

function CheckboxFelt(
  props: { name: keyof SchemaValues } & FieldHookConfig<any>
) {
  const [field] = useField({ ...props, type: "checkbox" });

  return <Checkbox {...field}>{props.children}</Checkbox>;
}

// TODO Se på uthenting av dato på korrekt format
function Datovelger() {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [fraDatoField, fraDatoMeta, fraDatoHelper] = useField("fraDato");
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [tilDatoField, tilDatoMeta, tilDatoHelper] = useField("tilDato");
  const { datepickerProps, toInputProps, fromInputProps } =
    UNSAFE_useRangeDatepicker({
      fromDate: new Date(),
      onRangeChange: (val) => {
        fraDatoHelper.setValue(val?.from);
        tilDatoHelper.setValue(val?.to);
      },
    });
  return (
    <UNSAFE_DatePicker {...datepickerProps}>
      <div style={{ display: "flex", gap: "5rem" }}>
        <DatoFelt name="fraDato" label="Fra dato" {...fromInputProps} />
        <DatoFelt name="tilDato" label="Til dato" {...toInputProps} />
      </div>
    </UNSAFE_DatePicker>
  );
}

function DatoFelt({
  name,
  label,
  ...rest
}: { name: keyof SchemaValues; label: string } & FieldHookConfig<any> & any) {
  const [_, meta] = useField({ name, ...rest });
  return (
    <UNSAFE_DatePicker.Input
      {...rest}
      label={label}
      name={name}
      error={meta.error}
    />
  );
}

export function OpprettTiltakstype() {
  const initialValues: SchemaValues = {
    tiltakstypenavn: "",
    fraDato: new Date(),
    tilDato: new Date(),
    tiltaksgruppekode: "",
    tiltakskode: "",
    rettTilTiltakspenger: false,
    administrasjonskode: "",
    kopiAvTilsagnsbrev: false,
    arkivkode: "",
    harAnskaffelse: false,
    rammeavtale: "",
    opplaringsgruppe: "",
    handlingsplan: "",
    harObligatoriskSluttdato: false,
    varighet: "",
    harStatusSluttdato: false,
    harStatusMeldeplikt: false,
    harStatusVedtak: false,
    harStatusIAAvtale: false,
    harStatusTilleggstonad: false,
    harStatusUtdanning: false,
    harAutomatiskTilsagnsbrev: false,
    harStatusBegrunnelseInnsok: false,
    harStatusHenvisningsbrev: false,
    harStatusKopibrev: false,
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

  return (
    <>
      <Link
        style={{ marginBottom: "1rem", display: "block" }}
        to="/tiltakstyper"
      >
        Tilbake til oversikt
      </Link>
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
          // TODO Må sende data til backend
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
              <Tekstfelt name="tiltakstypenavn" label="Navn på tiltakstype" />
              <SelectFelt name="tiltaksgruppekode" label="Tiltaksgruppekode">
                {tiltaksgruppekoder.map((kode) => (
                  <option key={kode} value={kode}>
                    {kode}
                  </option>
                ))}
              </SelectFelt>
              <Tekstfelt name="tiltakskode" label="Tiltakskode" />
              <Datovelger />

              <CheckboxFelt name="rettTilTiltakspenger">
                Rett på tiltakspenger
              </CheckboxFelt>
              <Tekstfelt
                name="administrasjonskode"
                label="Administrasjonskode"
              />
              <CheckboxFelt name="kopiAvTilsagnsbrev">
                Kopi av tilsagnsbrev
              </CheckboxFelt>
              <Tekstfelt name="arkivkode" label="Arkivkode" />
              <CheckboxFelt name="harAnskaffelse">Anskaffelse</CheckboxFelt>
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
              <CheckboxFelt name="harObligatoriskSluttdato">
                Obligatorisk sluttdato
              </CheckboxFelt>
              <Tekstfelt
                name="varighet"
                label="Varighet"
                hjelpetekst="Maks antall måneder"
              />

              <CheckboxFelt name="harStatusSluttdato">
                Status sluttdato
              </CheckboxFelt>
              <CheckboxFelt name="harStatusMeldeplikt">
                Status meldeplinkt
              </CheckboxFelt>
              <CheckboxFelt name="harStatusVedtak">Status vedtak</CheckboxFelt>
              <CheckboxFelt name="harStatusIAAvtale">
                Status IA Avtale
              </CheckboxFelt>
              <CheckboxFelt name="harStatusTilleggstonad">
                Status tilleggsstønad
              </CheckboxFelt>
              <CheckboxFelt name="harStatusUtdanning">
                Status utdanning
              </CheckboxFelt>

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
