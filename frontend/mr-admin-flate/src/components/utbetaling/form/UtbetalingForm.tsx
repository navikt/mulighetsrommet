import { Heading, HGrid } from "@navikt/ds-react";
import { useWatch } from "react-hook-form";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { FormGroup } from "@/layouts/FormGroup";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { NumberInput } from "@/components/skjema/NumberInput";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { addDuration } from "@mr/frontend-common/utils/date";
import { UtbetalingRequest } from "@tiltaksadministrasjon/api-client";
import { BetalingsinformasjonFields } from "./BetalingsinformasjonFields";

interface UtbetalingFormProps {
  id: string;
  onSubmit: () => void;
  arrangorId: string;
  startDato?: string | null;
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

function UtbetalingFields({ startDato }: { startDato?: string | null }) {
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
  return (
    <FormGroup>
      <Heading size="small" level="3">
        Betalingsinformasjon
      </Heading>
      <BetalingsinformasjonFields<UtbetalingRequest>
        arrangorId={arrangorId}
        kidNummerName="kidNummer"
      />
    </FormGroup>
  );
}
