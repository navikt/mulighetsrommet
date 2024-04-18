import { forwardRef } from "react";
import { Controller } from "react-hook-form";
import { DateInput } from "./DateInput";

export interface Props {
  label: string;
  readOnly?: boolean;
  fromDate: Date;
  toDate: Date;
  size?: "small" | "medium";
  format: "date" | "iso-string";
  placeholder?: string;
  invalidDatoEtterPeriode?: string;
}

export const ControlledDateInput = forwardRef(function ControlledDateInput(
  props: Props,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  _,
) {
  const {
    label,
    size,
    readOnly,
    format,
    fromDate,
    toDate,
    placeholder,
    invalidDatoEtterPeriode,
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
              fromDate={fromDate}
              toDate={toDate}
              onChange={onChange}
              error={error?.message}
              readOnly={readOnly}
              placeholder={placeholder}
              invalidDatoEtterPeriode={invalidDatoEtterPeriode}
              value={value}
            />
          );
        }}
      />
    </div>
  );
});
