import { Heading, HGrid, Link, TextField, VStack } from "@navikt/ds-react";
import { useWatch } from "react-hook-form";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { FormGroup } from "@/layouts/FormGroup";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { NumberInput } from "@/components/skjema/NumberInput";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { addDuration } from "@mr/frontend-common/utils/date";
import { useArrangorBetalingsinformasjon } from "@/api/arrangor/useArrangorBetalingsinformasjon";
import { Betalingsinformasjon, UtbetalingRequest } from "@tiltaksadministrasjon/api-client";

interface UtbetalingFormProps {
  id: string;
  onSubmit: () => void;
  arrangorId: string;
  startDato?: string;
}

export function UtbetalingForm({ id, onSubmit, arrangorId, startDato }: UtbetalingFormProps) {
  const korrigererUtbetaling = useWatch<UtbetalingRequest, "korrigererUtbetaling">({
    name: "korrigererUtbetaling",
  });

  return (
    <form id={id} onSubmit={onSubmit}>
      <TwoColumnGrid separator>
        {korrigererUtbetaling ? <KorreksjonFields /> : <UtbetalingFields startDato={startDato} />}
        <BetalingsinformasjonFormGroup arrangorId={arrangorId} />
      </TwoColumnGrid>
    </form>
  );
}

function KorreksjonFields() {
  return (
    <FormGroup>
      <UtbetalingPrisInput />
      <FormTextarea<UtbetalingRequest>
        label="Begrunnelse for korreksjon"
        name="korreksjonBegrunnelse"
        maxLength={250}
      />
    </FormGroup>
  );
}

function UtbetalingFields({ startDato }: { startDato?: string }) {
  return (
    <FormGroup>
      <HGrid columns={2}>
        <FormDateInput
          name="periodeStart"
          label="Periodestart"
          fromDate={startDato ? new Date(startDato) : undefined}
          toDate={addDuration(new Date(), { years: 5 })}
        />
        <FormDateInput
          name="periodeSlutt"
          label="Periodeslutt"
          fromDate={startDato ? new Date(startDato) : undefined}
          toDate={addDuration(new Date(), { years: 5 })}
        />
      </HGrid>
      <UtbetalingPrisInput />
      <FormTextField<UtbetalingRequest> label="Journalpost-ID i Gosys" name="journalpostId" />
      <FormTextarea<UtbetalingRequest> label="Kommentar" name="kommentar" maxLength={250} />
    </FormGroup>
  );
}

function UtbetalingPrisInput() {
  const valuta = useWatch<UtbetalingRequest, "pris.valuta">({
    name: "pris.valuta",
  });
  return <NumberInput<UtbetalingRequest> label={`Beløp (${valuta})`} name="pris.belop" />;
}

export function BetalingsinformasjonFormGroup({ arrangorId }: { arrangorId: string }) {
  const { data: betalingsinformasjon } = useArrangorBetalingsinformasjon(arrangorId);
  return (
    <FormGroup>
      <Heading size="small" level="3">
        Betalingsinformasjon
      </Heading>
      <BetalingsinformasjonFields betalingsinformasjon={betalingsinformasjon} />
    </FormGroup>
  );
}

function BetalingsinformasjonFields({
  betalingsinformasjon,
}: {
  betalingsinformasjon: Betalingsinformasjon;
}) {
  switch (betalingsinformasjon.type) {
    case "BBan":
      return (
        <VStack gap="space-8">
          <TextField
            size="small"
            label="Kontonummer til arrangør"
            readOnly
            value={betalingsinformasjon.kontonummer}
            description="Kontonummer hentes automatisk fra Altinn"
          />
          <small className="text-balance">
            Dersom kontonummer er feil må arrangør oppdatere kontonummer i Altinn. Les mer her om{" "}
            <EndreKontonummerLink />.
          </small>
          <FormTextField<UtbetalingRequest> label="Valgfritt KID-nummer" name="kidNummer" />
        </VStack>
      );
    case "IBan":
      return (
        <VStack gap="space-8" align="start">
          <Heading size="small">Bank</Heading>
          <TextField size="small" label="IBan" readOnly value={betalingsinformasjon.iban} />
          <TextField size="small" label="BIC/SWIFT" readOnly value={betalingsinformasjon.bic} />
          <TextField size="small" label="Banknavn" readOnly value={betalingsinformasjon.bankNavn} />
          <TextField
            size="small"
            label="Bank landkode"
            readOnly
            value={betalingsinformasjon.bankLandKode}
          />
          <small className="text-balance">
            Dersom informasjonen må oppdateres ta kontakt med utviklingsteamet.
          </small>
        </VStack>
      );
    case undefined:
      throw Error("unreachable");
  }
}

function EndreKontonummerLink() {
  return (
    <Link
      rel="noopener noreferrer"
      href="https://www.nav.no/arbeidsgiver/endre-kontonummer#hvordan"
      target="_blank"
    >
      endring av kontonummer for refusjoner fra Nav
    </Link>
  );
}
