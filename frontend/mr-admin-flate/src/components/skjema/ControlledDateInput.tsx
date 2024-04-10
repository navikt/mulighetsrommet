import { DatePicker, useDatepicker } from "@navikt/ds-react";
import { forwardRef, useState } from "react";
import { Controller } from "react-hook-form";
import { formaterDatoSomYYYYMMDD as formaterSomIsoDate } from "../../utils/Utils";
import styles from "./ControlledDateInput.module.scss";

export interface DateInputProps {
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
  props: DateInputProps,
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
    placeholder = "dd.mm.åååå",
    invalidDatoEtterPeriode = "Dato er etter gyldig periode",
    ...rest
  } = props;
  const [ugyldigDatoError, setUgyldigDatoError] = useState("");

  function formatDateForInput(date: Date): string {
    return date.toLocaleDateString("nb", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    });
  }

  return (
    <div>
      <Controller
        name={label}
        {...rest}
        render={({ field: { onChange, value }, fieldState: { error } }) => {
          const { datepickerProps, inputProps } =
            // FIXME
            // eslint-disable-next-line react-hooks/rules-of-hooks
            useDatepicker({
              onDateChange: (val) => {
                if (val) {
                  if (format === "iso-string") {
                    onChange(formaterSomIsoDate(val));
                  } else {
                    onChange(val);
                  }
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

          // Hvis value endres utenfor må man ekplisitt sette den her. Men for at
          // man fortsatt skal kunne redigere sjekker vi mot lengden til inputProps
          // sin value som vil være 10 etter den er validert, som trigrer onChange
          // og derfor er trygg og endre (fordi onChange endrer valuen utenfor, så
          // hvis den er endret igjen nå så må det ha vært utenfor).
          if ((inputProps.value + "").length === 10) {
            inputProps.value = formatDateForInput(new Date(value));
          }

          return (
            <DatePicker {...datepickerProps} dropdownCaption>
              <DatoFelt
                size={size}
                label={label}
                {...rest}
                {...inputProps}
                error={ugyldigDatoError || error?.message}
                readOnly={readOnly}
                placeholder={placeholder}
              />
            </DatePicker>
          );
        }}
      />
    </div>
  );
});

const DatoFelt = forwardRef(function DatoFeltInput(props: any, ref: any) {
  const { name, label, size, placeholder, ...rest } = props;
  return (
    <DatePicker.Input
      {...rest}
      label={label}
      name={name}
      size={size}
      ref={ref}
      className={styles.dato_input}
      placeholder={placeholder}
    />
  );
});
