import { BodyShort, Button, Heading } from "@navikt/ds-react";
import { Form, Formik } from "formik";

import { toFormikValidationSchema } from "zod-formik-adapter";
import styles from "../../Oversikt.module.scss";
import {
  CheckboxFelt,
  Datovelger, OptionalTiltakstypeSchemaValues,
  SelectFelt,
  Tekstfelt
} from "../../OpprettComponents";
import formStyles from "./OpprettTiltakstypePage.module.scss";
import {
  AdministrasjonskodeValue,
  HandlingsplanValue,
  OpplaeringsgruppeValue,
  OpprettTiltakstypeSchema,
  RammeavtaleValue,
  TiltaksgruppekodeValue,
} from "./OpprettTiltakstypeSchemaValidation";
import { Tilbakelenke } from "../../../components/navigering/Tilbakelenke";

export function OpprettTiltakstype() {
  const initialValues: OptionalTiltakstypeSchemaValues = {
    tiltakstypenavn: undefined,
    fraDato: undefined,
    tilDato: undefined,
    tiltaksgruppekode: undefined,
    tiltakskode: undefined,
    rettTilTiltakspenger: false,
    administrasjonskode: undefined,
    kopiAvTilsagnsbrev: false,
    harAnskaffelse: false,
    rammeavtale: undefined,
    opplaringsgruppe: undefined,
    handlingsplan: undefined,
    harObligatoriskSluttdato: false,
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
  const tiltaksgruppekoder: Record<
    TiltaksgruppekodeValue,
    | "Arbeidsforberedende trening"
    | "Tiltak i arbeidsmarkedsbedrift"
    | "Arbeidspraksis"
    | "Arbeidsrettet rehabilitering"
    | "Arbeidstrening"
    | "Avklaring"
    | "Behandling - lettere psykiske/sammensatte lidelser"
    | "Egenetablering"
    | "Forsøk"
    | "Lønnstilskudd"
    | "Oppfølging"
    | "Opplæring"
    | "Tilrettelegging"
    | "Tiltak under utfasing"
    | "Varig tilrettelagt arbeid"
  > = {
    AFT: "Arbeidsforberedende trening",
    AMB: "Tiltak i arbeidsmarkedsbedrift",
    ARBPRAKS: "Arbeidspraksis",
    ARBRREHAB: "Arbeidsrettet rehabilitering",
    ARBTREN: "Arbeidstrening",
    AVKLARING: "Avklaring",
    BEHPSSAM: "Behandling - lettere psykiske/sammensatte lidelser",
    ETAB: "Egenetablering",
    FORSOK: "Forsøk",
    LONNTILS: "Lønnstilskudd",
    OPPFOLG: "Oppfølging",
    OPPL: "Opplæring",
    TILRETTE: "Tilrettelegging",
    UTFAS: "Tiltak under utfasing",
    VARIGASV: "Varig tilrettelagt arbeid",
  };
  const opplaringsgrupper: Record<
    OpplaeringsgruppeValue,
    "AMO Kurskatalog" | "Utdanningsstruktur"
  > = {
    AMO: "AMO Kurskatalog",
    UTD: "Utdanningsstruktur",
  };

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
      <Tilbakelenke>Tilbake</Tilbakelenke>
      <Heading size="large">Opprett ny tiltakstype</Heading>
      <BodyShort className={styles.body} size="small">
        Her kan du opprette eller redigere en tiltakstype
      </BodyShort>
      <Formik<OptionalTiltakstypeSchemaValues>
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
                {Object.entries(tiltaksgruppekoder).map(([key, value]) => (
                  <option key={key} value={key}>
                    {value} - {key}
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
                {Object.entries(opplaringsgrupper).map(([key, value]) => (
                  <option key={key} value={key}>
                    {value} - {key}
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

              <CheckboxFelt name="harStatusSluttdato">
                Status sluttdato
              </CheckboxFelt>
              <CheckboxFelt name="harStatusMeldeplikt">
                Status meldeplikt
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
