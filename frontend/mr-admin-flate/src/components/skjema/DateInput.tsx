import { DatePicker, useDatepicker } from "@navikt/ds-react";
import { forwardRef, useEffect, useState } from "react";
import { formaterDatoSomYYYYMMDD as formaterSomIsoDate } from "../../utils/Utils";
import styles from "./DateInput.module.scss";

interface Props {
  label: string;
  readOnly?: boolean;
  onChange: (a0?: string | Date) => void;
  fromDate: Date;
  toDate: Date;
  size?: "small" | "medium";
  format: "date" | "iso-string";
  placeholder?: string;
  invalidDatoEtterPeriode?: string;
  value?: string;
  error?: string;
}

export const DateInput = forwardRef(function DateInput(
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
    onChange,
    value,
    error,
    placeholder = "dd.mm.åååå",
    invalidDatoEtterPeriode = "Dato er etter gyldig periode",
    ...rest
  } = props;
  const [ugyldigDatoError, setUgyldigDatoError] = useState("");

  const { datepickerProps, inputProps, setSelected } = useDatepicker({
    onDateChange: (val) => {
      if (format === "iso-string") {
        onChange(formaterSomIsoDate(val));
      } else {
        onChange(val);
      }
    },
    onValidate: (val) => {
      setUgyldigDatoError("");
      if (!val.isValidDate) {
        onChange(undefined);
        if (val.isBefore) {
          setUgyldigDatoError("Dato er før gyldig periode");
        } else if (val.isAfter) {
          setUgyldigDatoError(invalidDatoEtterPeriode);
        }
      }
    },
    allowTwoDigitYear: true,
    inputFormat: "dd.MM.yyyy",
    fromDate,
    toDate,
    defaultSelected: value ? new Date(value) : undefined,
  });

  useEffect(() => {
    if (value) {
      setSelected(new Date(value));
    }
  }, [value]);

  return (
    <DatePicker {...datepickerProps} dropdownCaption>
      <DatePicker.Input
        className={styles.dato_input}
        size={size}
        label={label}
        {...rest}
        {...inputProps}
        error={ugyldigDatoError || error}
        readOnly={readOnly}
        placeholder={placeholder}
      />
    </DatePicker>
  );
});
