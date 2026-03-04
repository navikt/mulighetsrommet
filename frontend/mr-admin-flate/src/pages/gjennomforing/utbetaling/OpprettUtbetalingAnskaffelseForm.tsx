import {
  GjennomforingDto,
  OpprettUtbetalingRequest,
  PrismodellDto,
} from "@tiltaksadministrasjon/api-client";
import { HGrid } from "@navikt/ds-react";
import { addDuration } from "@mr/frontend-common/utils/date";
import { FormGroup } from "@/components/skjema/FormGroup";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { NumberInput } from "@/components/skjema/NumberInput";
import { TextInput } from "@/components/skjema/TextInput";
import { TextareaInput } from "@/components/skjema/TextareaInput";
import { UtbetalingForm } from "@/pages/gjennomforing/utbetaling/UtbetalingForm";
import { useFormContext } from "react-hook-form";

interface Props {
  gjennomforing: GjennomforingDto;
  prismodell: PrismodellDto;
}

export function OpprettUtbetalingAnskaffelseForm({ gjennomforing, prismodell }: Props) {
  return (
    <UtbetalingForm gjennomforing={gjennomforing} prismodell={prismodell}>
      <UtbetalingAnskaffelseFormInput gjennomforing={gjennomforing} prismodell={prismodell} />
    </UtbetalingForm>
  );
}

function UtbetalingAnskaffelseFormInput({ gjennomforing, prismodell }: Props) {
  const {
    formState: { errors },
    setValue,
    getValues,
  } = useFormContext<OpprettUtbetalingRequest>();

  return (
    <FormGroup>
      <HGrid columns={2}>
        <ControlledDateInput
          label="Periodestart"
          fromDate={new Date(gjennomforing.startDato)}
          toDate={addDuration(new Date(), { years: 5 })}
          onChange={(val) => setValue("periodeStart", val)}
          defaultSelected={getValues("periodeStart")}
          error={errors.periodeStart?.message}
        />
        <ControlledDateInput
          label="Periodeslutt"
          fromDate={new Date(gjennomforing.startDato)}
          toDate={addDuration(new Date(), { years: 5 })}
          onChange={(val) => setValue("periodeSlutt", val)}
          defaultSelected={getValues("periodeSlutt")}
          error={errors.periodeSlutt?.message}
        />
      </HGrid>
      <NumberInput<OpprettUtbetalingRequest>
        label={`Beløp (${prismodell.valuta})`}
        name="pris.belop"
      />
      <TextInput<OpprettUtbetalingRequest> label="Journalpost-ID i Gosys" name="journalpostId" />
      <TextareaInput<OpprettUtbetalingRequest> label="Kommentar" name="beskrivelse" />
    </FormGroup>
  );
}
