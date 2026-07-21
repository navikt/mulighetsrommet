import { Checkbox, HelpText, HStack } from "@navikt/ds-react";
import { UtbetalingLinjeDto } from "@tiltaksadministrasjon/api-client";
import { utbetalingTekster } from "./UtbetalingTekster";
import { FormCheckbox } from "../skjema/FormCheckbox";

interface FormVariant {
  index: number;
}

interface DisplayVariant {
  linje: UtbetalingLinjeDto;
}

export function GjorOppTilsagnFormCheckbox({ index }: FormVariant) {
  return (
    <BaseGjorOppTilsagnCheckbox>
      <FormCheckbox name={`utbetalingLinjer.${index}.gjorOppTilsagn`} hideLabel>
        {utbetalingTekster.linje.gjorOpp.checkbox.label}
      </FormCheckbox>
    </BaseGjorOppTilsagnCheckbox>
  );
}

export function GjorOppTilsagnCheckbox({ linje }: DisplayVariant) {
  return (
    <BaseGjorOppTilsagnCheckbox>
      <Checkbox hideLabel readOnly checked={linje.gjorOppTilsagn}>
        {utbetalingTekster.linje.gjorOpp.checkbox.label}
      </Checkbox>
    </BaseGjorOppTilsagnCheckbox>
  );
}

function BaseGjorOppTilsagnCheckbox({ children }: React.PropsWithChildren) {
  return (
    <HStack gap="space-8">
      {children}
      <HelpText>{utbetalingTekster.linje.gjorOpp.checkbox.helpText}</HelpText>
    </HStack>
  );
}
