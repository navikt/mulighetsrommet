import { Checkbox, CheckboxProps, HelpText, HStack } from "@navikt/ds-react";
import { OpprettUtbetalingLinjerRequest, UtbetalingLinje } from "@tiltaksadministrasjon/api-client";
import { utbetalingTekster } from "./UtbetalingTekster";
import { useFormContext } from "react-hook-form";

interface FormVariant {
  index: number;
}

interface DisplayVariant {
  linje: UtbetalingLinje;
}

export function GjorOppTilsagnFormCheckbox({ index }: FormVariant) {
  const { register } = useFormContext<OpprettUtbetalingLinjerRequest>();
  return <BaseGjorOppTilsagnCheckbox {...register(`utbetalingLinjer.${index}.gjorOppTilsagn`)} />;
}

export function GjorOppTilsagnCheckbox({ linje }: DisplayVariant) {
  return <BaseGjorOppTilsagnCheckbox readOnly={true} checked={linje.gjorOppTilsagn} />;
}

function BaseGjorOppTilsagnCheckbox(props: Omit<CheckboxProps, "children">) {
  return (
    <HStack gap="space-8">
      <Checkbox hideLabel {...props}>
        {utbetalingTekster.linje.gjorOpp.checkbox.label}
      </Checkbox>
      <HelpText>{utbetalingTekster.linje.gjorOpp.checkbox.helpText}</HelpText>
    </HStack>
  );
}
