import { Checkbox, CheckboxProps, HelpText, HStack } from "@navikt/ds-react";
import { UtbetalingLinje } from "@tiltaksadministrasjon/api-client";
import { utbetalingTekster } from "./UtbetalingTekster";
import { useFormContext } from "react-hook-form";
import { RedigerUtbetalingLinjeFormValues } from "./helpers";

interface FormVariant {
  index: number;
}

interface DisplayVariant {
  linje: UtbetalingLinje;
}

export type GjorOppTilsagnCheckboxProps = FormVariant | DisplayVariant;

export function GjorOppTilsagnFormCheckbox({ index }: FormVariant) {
  const { register } = useFormContext<RedigerUtbetalingLinjeFormValues>();
  return <BaseGjorOppTilsagnCheckbox {...register(`formLinjer.${index}.gjorOppTilsagn`)} />;
}

export function GjorOppTilsagnCheckbox({ linje }: DisplayVariant) {
  return <BaseGjorOppTilsagnCheckbox readOnly={true} checked={linje.gjorOppTilsagn} />;
}

function BaseGjorOppTilsagnCheckbox(props: Omit<CheckboxProps, "children">) {
  return (
    <HStack gap="2">
      <Checkbox hideLabel {...props}>
        {utbetalingTekster.delutbetaling.gjorOpp.checkbox.label}
      </Checkbox>
      <HelpText>{utbetalingTekster.delutbetaling.gjorOpp.checkbox.helpText}</HelpText>
    </HStack>
  );
}
