import { UtbetalingLinje } from "@tiltaksadministrasjon/api-client";
import { useFormContext } from "react-hook-form";
import { RedigerUtbetalingLinjeFormValues } from "./helpers";
import { TextField, TextFieldProps } from "@navikt/ds-react";
import { utbetalingTekster } from "./UtbetalingTekster";

export type UtbetalingBelopInputProps = FormVariant | DisplayVariant;

interface FormVariant {
  type: "form";
  index: number;
}

interface DisplayVariant {
  type: "readOnly";
  linje: UtbetalingLinje;
}

export function UtbetalingBelopInput(props: UtbetalingBelopInputProps) {
  switch (props.type) {
    case "form":
      return <UtbetalingBelopFormInput {...props} />;
    case "readOnly":
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
