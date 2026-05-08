import { UNSAFE_Combobox } from "@navikt/ds-react";
import { FieldValues, Path, useController, useFormContext } from "react-hook-form";
import { useState } from "react";
import { useSokSertifiseringer } from "@/api/janzz/useSokSertifiseringer";
import { gjennomforingTekster } from "../ledetekster/gjennomforingLedetekster";
import { AmoKategoriseringBransjeOgYrkesrettetSertifisering as Sertifisering } from "@tiltaksadministrasjon/api-client";

export function SertifiseringerSkjema<T extends FieldValues>(props: {
  path: Path<T>;
  options?: Sertifisering[];
}) {
  const { path, options } = props;
  const [janzzQuery, setJanzzQuery] = useState<string>("");
  const { data: sertifiseringerFraSok } = useSokSertifiseringer(janzzQuery);
  const { control } = useFormContext<T>();
  const { field, fieldState } = useController({ name: path, control });

  const sertifiseringer: Sertifisering[] = Array.isArray(field.value) ? field.value : [];

  const selectedOptions = sertifiseringer.map((s) => ({
    value: String(s.konseptId),
    label: s.label,
  }));

  const selectedKonseptIds = new Set(sertifiseringer.map((s) => s.konseptId));
  const source = options ?? sertifiseringerFraSok ?? [];
  const unselected = source
    .filter((s) => !selectedKonseptIds.has(s.konseptId))
    .map((s) => ({ value: String(s.konseptId), label: s.label }));
  const allOptions = [...selectedOptions, ...unselected];

  function handleToggleSelected(optionValue: string, isSelected: boolean) {
    if (isSelected) {
      const source = options ?? sertifiseringerFraSok ?? [];
      const found = source.find((s) => String(s.konseptId) === optionValue);
      if (found) {
        field.onChange([...sertifiseringer, { konseptId: found.konseptId, label: found.label }]);
      }
    } else {
      field.onChange(sertifiseringer.filter((s) => String(s.konseptId) !== optionValue));
    }
  }

  return (
    <UNSAFE_Combobox
      size="small"
      isMultiSelect
      placeholder="Søk etter sertifiseringer"
      label={gjennomforingTekster.sertifiseringerLabel}
      name={field.name}
      options={allOptions}
      selectedOptions={selectedOptions}
      filteredOptions={allOptions}
      error={fieldState.error?.message}
      onChange={(value) => {
        if (!options) {
          setJanzzQuery(value);
        }
      }}
      onToggleSelected={handleToggleSelected}
    />
  );
}
