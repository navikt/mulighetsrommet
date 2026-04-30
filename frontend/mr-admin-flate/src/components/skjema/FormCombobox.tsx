import { type ComboboxProps, UNSAFE_Combobox } from "@navikt/ds-react";
import {
  ControllerRenderProps,
  type FieldPath,
  type FieldValues,
  type RegisterOptions,
  useController,
  useFormContext,
} from "react-hook-form";

type Option = { label: string; value: string };

type FormComboboxProps<TFieldValues extends FieldValues> = Omit<
  ComboboxProps,
  "value" | "error" | "name" | "options" | "selectedOptions"
> & {
  name: FieldPath<TFieldValues>;
  options: Option[];
  rules?: RegisterOptions<TFieldValues>;
};

export function FormCombobox<TFieldValues extends FieldValues>({
  name,
  options,
  rules,
  size = "small",
  isMultiSelect = false,
  onToggleSelected: onToggleSelectedProp,
  ...props
}: FormComboboxProps<TFieldValues>) {
  const { control } = useFormContext<TFieldValues>();
  const { field, fieldState } = useController({ name, control, rules });

  const { resolvedOptions, selectedOptions, onToggleSelected } = isMultiSelect
    ? resolveMultiSelect(field, options)
    : resolveSingleSelect(field, options);

  function handleToggleSelected(option: string, isSelected: boolean, isCustomOption: boolean) {
    onToggleSelected(option, isSelected);
    onToggleSelectedProp?.(option, isSelected, isCustomOption);
  }

  return (
    <UNSAFE_Combobox
      {...props}
      size={size}
      name={field.name}
      options={resolvedOptions}
      selectedOptions={selectedOptions}
      error={fieldState.error?.message}
      isMultiSelect={isMultiSelect}
      onToggleSelected={handleToggleSelected}
    />
  );
}

function resolveSingleSelect<
  TFieldValues extends FieldValues,
  TName extends FieldPath<TFieldValues>,
>(field: ControllerRenderProps<TFieldValues, TName>, options: Option[]) {
  const value = field.value as string | undefined;
  const resolvedOptions = getResolvedOptionsSingle(options, value);
  const selectedOptions = resolvedOptions.filter((o) => o.value === value);
  function onToggleSelected(optionValue: string, isSelected: boolean) {
    field.onChange(isSelected ? optionValue : null);
  }
  return { resolvedOptions, selectedOptions, onToggleSelected };
}

function getResolvedOptionsSingle(options: Option[], value: string | undefined) {
  if (typeof value === "string" && value && !options.some((o) => o.value === value)) {
    return [...options, { value: value, label: value }];
  }
  return options;
}

function resolveMultiSelect<
  TFieldValues extends FieldValues,
  TName extends FieldPath<TFieldValues>,
>(field: ControllerRenderProps<TFieldValues, TName>, options: Option[]) {
  const value = Array.isArray(field.value) ? (field.value as string[]) : ([] as string[]);
  const resolvedOptions = getResolvedOptionsMulti(options, value);
  const selectedOptions = resolvedOptions.filter((o) => value.includes(o.value));
  function onToggleSelected(optionValue: string, isSelected: boolean) {
    const currentValues = Array.isArray(field.value) ? field.value : [];
    field.onChange(
      isSelected
        ? [...currentValues, optionValue]
        : currentValues.filter((v: string) => v !== optionValue),
    );
  }
  return { resolvedOptions, selectedOptions, onToggleSelected };
}

function getResolvedOptionsMulti(options: Option[], values: string[]) {
  const missing = values.filter((value) => !options.some((o) => o.value === value));
  return missing.length > 0
    ? [...options, ...missing.map((value) => ({ value, label: value }))]
    : options;
}
