import { useId } from "react";
import {
  Button,
  type ComboboxProps,
  HStack,
  Label,
  UNSAFE_Combobox,
  VStack,
} from "@navikt/ds-react";
import {
  type FieldPath,
  type FieldValues,
  type RegisterOptions,
  useController,
  useFormContext,
} from "react-hook-form";

type Option = { label: string; value: string };

type FormComboboxMultiProps<TFieldValues extends FieldValues> = Omit<
  ComboboxProps,
  "value" | "error" | "name" | "options" | "selectedOptions" | "isMultiSelect"
> & {
  name: FieldPath<TFieldValues>;
  options: Option[];
  rules?: RegisterOptions<TFieldValues>;
  selectAll?: boolean;
};

export function FormComboboxMulti<TFieldValues extends FieldValues>({
  name,
  options,
  rules,
  size = "small",
  selectAll = false,
  onToggleSelected: onToggleSelectedProp,
  label,
  id: idProp,
  ...props
}: FormComboboxMultiProps<TFieldValues>) {
  const generatedId = useId();
  const id = idProp ?? generatedId;

  const { control } = useFormContext<TFieldValues>();
  const { field, fieldState } = useController({ name, control, rules });

  const values = Array.isArray(field.value) ? (field.value as string[]) : [];
  const resolvedOptions = getResolvedOptions(options, values);
  const selectedOptions = resolvedOptions.filter((o) => values.includes(o.value));

  function handleToggleSelected(option: string, isSelected: boolean, isCustomOption: boolean) {
    const selectedValues = isSelected ? [...values, option] : values.filter((v) => v !== option);
    field.onChange(selectedValues);
    onToggleSelectedProp?.(option, isSelected, isCustomOption);
  }

  if (selectAll) {
    const allSelected = options.length > 0 && options.every((o) => values.includes(o.value));
    return (
      <VStack gap="space-4">
        <HStack justify="space-between" align="center">
          <Label htmlFor={id} size={size}>
            {label}
          </Label>
          <Button
            size="xsmall"
            variant="tertiary"
            type="button"
            onClick={() => field.onChange(allSelected ? [] : options.map((o) => o.value))}
          >
            {allSelected ? "Fjern alle" : "Velg alle"}
          </Button>
        </HStack>
        <UNSAFE_Combobox
          {...props}
          id={id}
          label={label}
          hideLabel
          size={size}
          isMultiSelect
          name={field.name}
          options={resolvedOptions}
          selectedOptions={selectedOptions}
          error={fieldState.error?.message}
          onToggleSelected={handleToggleSelected}
        />
      </VStack>
    );
  }

  return (
    <UNSAFE_Combobox
      {...props}
      id={id}
      label={label}
      size={size}
      isMultiSelect
      name={field.name}
      options={resolvedOptions}
      selectedOptions={selectedOptions}
      error={fieldState.error?.message}
      onToggleSelected={handleToggleSelected}
    />
  );
}

function getResolvedOptions(options: Option[], values: string[]) {
  const missing = values.filter((value) => !options.some((o) => o.value === value));
  return missing.length > 0
    ? [...options, ...missing.map((value) => ({ value, label: value }))]
    : options;
}
