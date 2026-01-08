import { yyyyMMddSafeFormatting } from "@mr/frontend-common/utils/date";
import { DatePicker, useDatepicker } from "@navikt/ds-react";
import { useState } from "react";

export interface ControlledDateInputProps {
  label: string;
  hideLabel?: boolean;
  readOnly?: boolean;
  fromDate?: Date;
  toDate?: Date;
  required?: boolean;
  onChange: (date: string) => void;
  defaultSelected?: string | null;
  error?: string;
  size?: "small" | "medium";
  placeholder?: string;
  invalidDatoEtterPeriode?: string;
  invalidDatoForTidlig?: string;
}

export const ControlledDateInput = ({
  label,
  hideLabel = false,
  size = "small",
  readOnly = false,
  onChange,
  defaultSelected,
  required = false,
  error,
  fromDate,
  toDate,
  placeholder = "dd.mm.åååå",
  invalidDatoEtterPeriode = "Dato er etter gyldig periode",
  invalidDatoForTidlig = "Dato er før gyldig periode",
}: ControlledDateInputProps) => {
  const [ugyldigDatoError, setUgyldigDatoError] = useState("");

  const { datepickerProps, inputProps } = useDatepicker({
    onDateChange: (val: Date | undefined) => {
      onChange(val ? yyyyMMddSafeFormatting(val) : "");
    },
    onValidate: (validation: { isValidDate: boolean; isBefore: boolean; isAfter: boolean }) => {
      setUgyldigDatoError("");
      if (!validation.isValidDate) {
        if (validation.isBefore) {
          setUgyldigDatoError(invalidDatoForTidlig);
        } else if (validation.isAfter) {
          setUgyldigDatoError(invalidDatoEtterPeriode);
        }
      }
    },
    required: required,
    allowTwoDigitYear: true,
    inputFormat: "dd.MM.yyyy",
    defaultSelected: defaultSelected ? new Date(defaultSelected) : undefined,
    fromDate,
    toDate,
  });

  return (
    <DatePicker {...datepickerProps} dropdownCaption>
      <DatePicker.Input
        {...inputProps}
        error={ugyldigDatoError || error}
        size={size}
        label={label}
        hideLabel={hideLabel}
        readOnly={readOnly}
        placeholder={placeholder}
      />
    </DatePicker>
  );
};
