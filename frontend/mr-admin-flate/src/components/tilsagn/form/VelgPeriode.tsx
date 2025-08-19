import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";
import { HGrid } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { InferredTilsagn } from "./TilsagnSchema";
import { addDuration } from "@mr/frontend-common/utils/date";

interface Props {
  startDato: string;
}

export function VelgPeriode(props: Props) {
  const {
    setValue,
    getValues,
    formState: { errors },
  } = useFormContext<InferredTilsagn>();

  return (
    <HGrid columns={2}>
      <ControlledDateInput
        label={tilsagnTekster.periode.start.label}
        fromDate={new Date(props.startDato)}
        toDate={addDuration(new Date(), { years: 50 })}
        defaultSelected={getValues("periodeStart")}
        onChange={(val) => setValue("periodeStart", val)}
        error={errors.periodeStart?.message}
      />
      <ControlledDateInput
        label={tilsagnTekster.periode.slutt.label}
        fromDate={new Date(props.startDato)}
        toDate={addDuration(new Date(), { years: 50 })}
        defaultSelected={getValues("periodeSlutt")}
        onChange={(val) => setValue("periodeSlutt", val)}
        error={errors.periodeSlutt?.message}
      />
    </HGrid>
  );
}
