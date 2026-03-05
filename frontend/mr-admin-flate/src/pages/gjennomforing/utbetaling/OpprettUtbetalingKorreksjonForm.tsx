import {
  GjennomforingDto,
  OpprettUtbetalingRequest,
  PrismodellDto,
} from "@tiltaksadministrasjon/api-client";
import { HGrid } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { addDuration } from "@mr/frontend-common/utils/date";
import { FormGroup } from "@/components/skjema/FormGroup";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { NumberInput } from "@/components/skjema/NumberInput";
import { TextareaInput } from "@/components/skjema/TextareaInput";
import { UtbetalingForm } from "@/pages/gjennomforing/utbetaling/UtbetalingForm";

interface Props {
  gjennomforing: GjennomforingDto;
  prismodell: PrismodellDto;
}

export function OpprettUtbetalingKorreksjonForm({ gjennomforing, prismodell }: Props) {
  return (
    <UtbetalingForm gjennomforing={gjennomforing} prismodell={prismodell}>
      <UtbetalingKorreksjonFormInput gjennomforing={gjennomforing} prismodell={prismodell} />
    </UtbetalingForm>
  );
}

function UtbetalingKorreksjonFormInput({ gjennomforing, prismodell }: Props) {
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
      <TextareaInput<OpprettUtbetalingRequest>
        label="Begrunnelse for utbetaling"
        name="beskrivelse"
      />
    </FormGroup>
  );
}
