import { addYear } from "@/utils/Utils";
import { HGrid } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";

interface Props {
  startDato: string;
}

export function VelgPeriode(props: Props) {
  const { register, control } = useFormContext<{ periodeStart: string; periodeSlutt: string }>();

  return (
    <HGrid columns={2}>
      <ControlledDateInput
        label={tilsagnTekster.periode.start.label}
        fromDate={new Date(props.startDato)}
        toDate={addYear(new Date(), 50)}
        format="iso-string"
        {...register("periodeStart")}
        size="small"
        control={control}
      />
      <ControlledDateInput
        label={tilsagnTekster.periode.slutt.label}
        fromDate={new Date(props.startDato)}
        toDate={addYear(new Date(), 50)}
        format="iso-string"
        {...register("periodeSlutt")}
        size="small"
        control={control}
      />
    </HGrid>
  );
}
