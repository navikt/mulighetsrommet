import { DateInput } from "@/components/skjema/DateInput";
import { forwardRef } from "react";
import { Controller, Control, FieldValues, Path } from "react-hook-form";

export interface ControlledDateInputProps<T extends FieldValues> {
  name: Path<T>; // Type-safe field name
  control: Control<T>; // Required react-hook-form control
  label: string;
  hideLabel?: boolean;
  readOnly?: boolean;
  fromDate: Date;
  toDate: Date;
  size?: "small" | "medium";
  format: "date" | "iso-string";
  placeholder?: string;
  invalidDatoEtterPeriode?: string;
  invalidDatoForTidlig?: string;
}

export const ControlledDateInput = forwardRef(function ControlledDateInput<T extends FieldValues>(
  props: ControlledDateInputProps<T>,
) {
  const {
    name,
    control,
    label,
    hideLabel = false,
    size,
    readOnly,
    format,
    fromDate,
    toDate,
    placeholder,
    invalidDatoEtterPeriode,
    invalidDatoForTidlig,
  } = props;

  return (
    <div>
      <Controller
        name={name}
        control={control}
        render={({ field: { onChange, value }, fieldState: { error } }) => {
          return (
            <DateInput
              size={size}
              format={format}
              label={label}
              hideLabel={hideLabel}
              fromDate={fromDate}
              toDate={toDate}
              onChange={onChange}
              error={error?.message}
              readOnly={readOnly}
              placeholder={placeholder}
              invalidDatoEtterPeriode={invalidDatoEtterPeriode}
              invalidDatoForTidlig={invalidDatoForTidlig}
              value={value}
            />
          );
        }}
      />
    </div>
  );
});
