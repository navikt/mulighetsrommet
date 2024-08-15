import { forwardRef, useMemo } from "react";
import { Controller } from "react-hook-form";
import { DateInput } from "@/components/skjema/DateInput";

export interface Props {
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

export const ControlledDateInput = forwardRef(function ControlledDateInput(
  props: Props,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  _,
) {
  const {
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
    ...rest
  } = props;

  return (
    <div>
      <Controller
        name={label}
        {...rest}
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
