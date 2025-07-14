import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";
import { HGrid } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { InferredTilsagn } from "./TilsagnSchema";
import { addYears } from "date-fns";
import { parseDate } from "@mr/frontend-common/utils/date";

interface Props {
  startDato: string;
}

export function VelgPeriode(props: Props) {
  const { register, control } = useFormContext<InferredTilsagn>();

  return (
    <HGrid columns={2}>
      <ControlledDateInput
        label={tilsagnTekster.periode.start.label}
        fromDate={parseDate(props.startDato)!}
        toDate={addYears(new Date(), 50)}
        format="iso-string"
        {...register("periodeStart")}
        size="small"
        control={control}
      />
      <ControlledDateInput
        label={tilsagnTekster.periode.slutt.label}
        fromDate={new Date(props.startDato)}
        toDate={addYears(new Date(), 50)}
        format="iso-string"
        {...register("periodeSlutt")}
        size="small"
        control={control}
      />
    </HGrid>
  );
}
