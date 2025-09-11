import { UtbetalingLinje } from "@tiltaksadministrasjon/api-client";
import { useFormContext } from "react-hook-form";
import { RedigerUtbetalingLinjeFormValues } from "./helpers";
import { TextField, TextFieldProps } from "@navikt/ds-react";
import { utbetalingTekster } from "./UtbetalingTekster";

export type UtbetalingBelopInputProps = FormVariant | DisplayVariant;

interface FormVariant {
  index: number;
}

interface DisplayVariant {
  linje: UtbetalingLinje;
}

function isFormVariant(props: UtbetalingBelopInputProps): props is FormVariant {
  return "index" in props;
}

export function UtbetalingBelopInput(props: UtbetalingBelopInputProps) {
  if (isFormVariant(props)) {
    return <UtbetalingBelopFormInput {...props} />;
  } else {
    return <UtbetalingBelopDisplayInput {...props} />;
  }
}

function UtbetalingBelopFormInput({ index }: { index: number }) {
  const { register } = useFormContext<RedigerUtbetalingLinjeFormValues>();
  return <BaseUtbetalingBelopInput {...register(`formLinjer.${index}.belop`)} />;
}

function UtbetalingBelopDisplayInput({ linje }: DisplayVariant) {
  return <BaseUtbetalingBelopInput readOnly={true} value={linje.belop} />;
}

function BaseUtbetalingBelopInput(props: Omit<TextFieldProps, "label">) {
  return (
    <TextField
      size="small"
      style={{ maxWidth: "6rem" }}
      hideLabel
      inputMode="numeric"
      label={utbetalingTekster.delutbetaling.belop.label}
      {...props}
    />
  );
}
