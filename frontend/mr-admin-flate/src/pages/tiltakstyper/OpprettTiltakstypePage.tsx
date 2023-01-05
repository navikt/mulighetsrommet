import {
  BodyLong,
  Button,
  Heading,
  Select,
  Switch,
  TextField,
} from "@navikt/ds-react";
import { FieldHookConfig, Form, Formik, useField } from "formik";
import { z } from "zod";
import { toFormikValidationSchema } from "zod-formik-adapter";
import styles from "../tiltaksgjennomforinger/Oversikt.module.scss";

const Schema = z.object({
  tiltakstypenavn: z.string({ required_error: "Tiltakstypen må ha et navn" }),
  tiltaksgruppekode: z.string({
    required_error: "Tiltaksgruppekode må være satt",
  }),
  tiltakskode: z.string({ required_error: "Tiltakskode må være satt" }),
  rettTilTiltakspenger: z.boolean().default(false),
  administrasjonskode: z.string({
    required_error: "Du må sette en administrasjonskode",
  }),
  kopiAvTilsagnsbrev: z.boolean().default(false),
  arkivkode: z.string({ required_error: "Du må sette en arkivkode" }),
  harAnskaffelse: z.boolean().default(false),
  rammeavtale: z.string({ required_error: "Du må velge en rammeavtale" }),
  opplaringsgruppe: z.string({
    required_error: "Du må velge en opplæringsgruppe",
  }),
  handlingsplan: z.string({ required_error: "Du må velge en handlingsplan" }),
  harObligatoriskSluttdato: z.boolean().default(false),
  varighet: z.string({ required_error: "Du må sette en varighet" }),
  harStatusSluttdato: z.boolean().default(false),
  harStatusMeldeplikt: z.boolean().default(false),
  harStatusVedtak: z.boolean().default(false),
  harStatusIAAvtale: z.boolean().default(false),
  harStatusTilleggstonad: z.boolean().default(false),
  harStatusUtdanning: z.boolean().default(false),
  harAutomatiskTilsagsnbrev: z.boolean().default(false),
  harStatusBegrunnelseInnsok: z.boolean().default(false),
  harStatusHenvisningsbrev: z.boolean().default(false),
  harStatusKopibrev: z.boolean().default(false),
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
      error={meta.error}
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
    <Select size="small" label={label} {...field} error={meta.error}>
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

export function OpprettTiltakstype() {
  const initialValues: SchemaValues = {
    tiltakstypenavn: "",
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
    harAutomatiskTilsagsnbrev: false,
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
          setTimeout(() => {
            alert(JSON.stringify(values, null, 2));
            actions.setSubmitting(false);
          }, 150);
        }}
      >
        {() => (
          <Form>
            <BodyLong spacing>
              <Tekstfelt name="tiltakstypenavn" label="Navn på tiltakstype" />
            </BodyLong>
            <BodyLong spacing>
              <SelectFelt name="tiltaksgruppekode" label="Tiltaksgruppekode">
                {tiltaksgruppekoder.map((kode) => (
                  <option key={kode} value={kode}>
                    {kode}
                  </option>
                ))}
              </SelectFelt>
            </BodyLong>
            <Tekstfelt name="tiltakskode" label="Tiltakskode" />
            <SwitchFelt name="rettTilTiltakspenger">
              Rett til tiltakspenger
            </SwitchFelt>
            <Tekstfelt name="administrasjonskode" label="Administrasjonskode" />
            <SwitchFelt name="kopiAvTilsagnsbrev">
              Kopi av tilsagnsbrev
            </SwitchFelt>
            <Tekstfelt name="arkivkode" label="Arkivkode" />
            <SwitchFelt name="harAnskaffelse">Anskaffelse</SwitchFelt>
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
            <SwitchFelt name="harObligatoriskSluttdato">
              Obligatorisk sluttdato
            </SwitchFelt>
            <Tekstfelt
              name="varighet"
              label="Varighet"
              hjelpetekst="Maks antall måneder"
            />

            <SwitchFelt name="harStatusSluttdato">Status sluttdato</SwitchFelt>
            <SwitchFelt name="harStatusMeldeplikt">
              Status meldeplinkt
            </SwitchFelt>
            <SwitchFelt name="harStatusVedtak">Status vedtak</SwitchFelt>
            <SwitchFelt name="harStatusIAAvtale">Status IA Avtale</SwitchFelt>
            <SwitchFelt name="harStatusTilleggstonad">
              Status tilleggsstønad
            </SwitchFelt>
            <SwitchFelt name="harStatusUtdanning">Status utdanning</SwitchFelt>

            <SwitchFelt name="harAutomatiskTilsagsnbrev">
              Automatisk tilsagnsbrev
            </SwitchFelt>
            <SwitchFelt name="harStatusBegrunnelseInnsok">
              Status begrunnelse innsøk
            </SwitchFelt>
            <SwitchFelt name="harStatusHenvisningsbrev">
              Status henvisningsbrev
            </SwitchFelt>
            <SwitchFelt name="harStatusKopibrev">Status kopibrev</SwitchFelt>
            <Button type="submit">Publiser</Button>
          </Form>
        )}
      </Formik>
    </>
  );
}
