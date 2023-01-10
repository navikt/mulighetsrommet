import { BodyLong, Button, Heading } from "@navikt/ds-react";
import { Form, Formik } from "formik";
import { Link } from "react-router-dom";

import { toFormikValidationSchema } from "zod-formik-adapter";
import styles from "../../tiltaksgjennomforinger/Oversikt.module.scss";
import {
  CheckboxFelt,
  Datovelger,
  SelectFelt,
  Tekstfelt,
} from "./OpprettTiltakstypeComponents";
import formStyles from "./OpprettTiltakstypePage.module.scss";
import {
  AdministrasjonskodeValue,
  HandlingsplanValue,
  OpprettTiltakstypeSchema,
  OptionalSchemaValues,
  RammeavtaleValue,
} from "./OpprettTiltakstypeSchemaValidation";

export function OpprettTiltakstype() {
  const initialValues: OptionalSchemaValues = {
    tiltakstypenavn: undefined,
    fraDato: undefined,
    tilDato: undefined,
    tiltaksgruppekode: undefined,
    tiltakskode: undefined,
    rettTilTiltakspenger: false,
    administrasjonskode: undefined,
    kopiAvTilsagnsbrev: false,
    arkivkode: undefined,
    harAnskaffelse: false,
    rammeavtale: undefined,
    opplaringsgruppe: undefined,
    handlingsplan: undefined,
    harObligatoriskSluttdato: false,
    varighet: undefined,
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
  const opplaringsgrupper = [
    { id: 1, navn: "Den beste opplæringsgruppen" },
    { id: 2, navn: "Den nest beste opplæringsgruppen" },
  ];

  const administrasjonskoder: Record<
    AdministrasjonskodeValue,
    "Arbeidsmarkedopplæring" | "Individuelt tiltak" | "Institusjonelt tiltak"
  > = {
    AMO: "Arbeidsmarkedopplæring",
    IND: "Individuelt tiltak",
    INST: "Institusjonelt tiltak",
  };

  const rammeavtaler: Record<RammeavtaleValue, string> = {
    KAN: "KAN",
    SKAL: "SKAL",
    IKKE: "IKKE",
  };

  const handlingsplaner: Record<HandlingsplanValue, string> = {
    LAG: "Lage handlingsplan",
    SOK: "Søke inn eller opprette deltakelse",
    TIL: "Ingen",
  };

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
      <Formik<OptionalSchemaValues>
        initialValues={initialValues}
        validationSchema={toFormikValidationSchema(OpprettTiltakstypeSchema)}
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
              <SelectFelt
                name="administrasjonskode"
                label="Administrasjonskode"
              >
                {Object.entries(administrasjonskoder).map(([key, value]) => (
                  <option key={key} value={key}>
                    {value} - {key}
                  </option>
                ))}
              </SelectFelt>
              <CheckboxFelt name="kopiAvTilsagnsbrev">
                Kopi av tilsagnsbrev
              </CheckboxFelt>
              <Tekstfelt name="arkivkode" label="Arkivkode" />
              <CheckboxFelt name="harAnskaffelse">Anskaffelse</CheckboxFelt>
              <SelectFelt name="rammeavtale" label="Rammeavtale">
                {Object.entries(rammeavtaler).map(([key, value]) => (
                  <option key={key} value={key}>
                    {value}
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
                {Object.entries(handlingsplaner).map(([key, value]) => (
                  <option key={key} value={key}>
                    {value}
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
