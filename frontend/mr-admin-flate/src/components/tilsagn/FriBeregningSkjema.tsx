import { TextField } from "@navikt/ds-react";
import { NumericFormat } from "react-number-format";
import { InferredOpprettTilsagnSchema } from "./OpprettTilsagnSchema";
import { DeepPartial, useFormContext } from "react-hook-form";

export function FriBeregningSkjema() {
  const {
    watch,
    setValue,
    formState: { errors },
  } = useFormContext<DeepPartial<InferredOpprettTilsagnSchema>>();

  return (
    <NumericFormat
      size="small"
      error={errors.belop?.message}
      label="Beløp i kroner"
      customInput={TextField}
      value={watch("belop") ?? 0}
      valueIsNumericString
      thousandSeparator
      suffix=" kr"
      onValueChange={(e) => {
        setValue("belop", Number.parseInt(e.value));
      }}
    />
  );
}
