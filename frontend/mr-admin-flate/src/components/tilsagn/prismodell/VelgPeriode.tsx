import { addYear } from "@/utils/Utils";
import { HGrid } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";

interface Props {
  startDato: string;
}

export function VelgPeriode(props: Props) {
  const { register } = useFormContext<{ periodeStart: string; periodeSlutt: string }>();

  return (
    <HGrid columns={2}>
      <ControlledDateInput
        label="Dato fra"
        fromDate={new Date(props.startDato)}
        toDate={addYear(new Date(), 50)}
        format="iso-string"
        {...register("periodeStart")}
        size="small"
      />
      <ControlledDateInput
        label="Dato til"
        fromDate={new Date(props.startDato)}
        toDate={addYear(new Date(), 50)}
        format="iso-string"
        {...register("periodeSlutt")}
        size="small"
      />
    </HGrid>
  );
}
