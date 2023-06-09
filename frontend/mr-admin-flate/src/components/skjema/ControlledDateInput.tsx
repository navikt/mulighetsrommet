import { formaterDato } from "../../utils/Utils";
import styles from "./ControlledDateInput.module.scss"
import { forwardRef } from "react";
import { Controller } from "react-hook-form";
import { UNSAFE_DatePicker, UNSAFE_useDatepicker } from "@navikt/ds-react";

export interface DateInputProps {
  label: string;
  readOnly?: boolean;
  size?: "small" | "medium";
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const ControlledDateInput = forwardRef((props: DateInputProps, _) => {
  const { label, size, readOnly, ...rest } = props;

  return (
    <div>
      <Controller
        name={label}
        {...rest}
        render={({
          field: { onChange, value },
          fieldState: { error },
        }) => {
          const {
            datepickerProps: startdatoProps,
            inputProps: startdatoInputProps,
            selectedDay: selectedStartdato,
          } = UNSAFE_useDatepicker({
            onDateChange: (val: any) => {
              if (val) {
                onChange(val);
              } else {
                onChange(null);
              }
            },
            allowTwoDigitYear: true,
            inputFormat: "dd.MM.yyyy",
            fromDate: pastDate(),
            toDate: futureDate(),
            defaultSelected: value ? new Date(value) : undefined,
          });

          return (
            <UNSAFE_DatePicker {...startdatoProps} dropdownCaption>
              <DatoFelt
                size={size}
                label={label}
                {...rest}
                error={error?.message}
                {...startdatoInputProps}
                value={
                  selectedStartdato ? formaterDato(selectedStartdato) : undefined
                }
                readOnly={readOnly}
              />
            </UNSAFE_DatePicker>
          );
        }}
      />
    </div>
  );
});

ControlledDateInput.displayName = "ControlledDateInput";

const DatoFelt = forwardRef(function DatoFeltInput(props: any, ref: any) {
  const { name, label, size, ...rest } = props;
  return (
    <UNSAFE_DatePicker.Input
      {...rest}
      label={label}
      name={name}
      size={size}
      ref={ref}
      disabled={rest.readOnly}
      className={styles.dato_input}
    />
  );
});

const offsetAntallAar = 3;

const pastDate = () => {
  const newDate = new Date();
  const yearsAgo = newDate.setFullYear(
    newDate.getFullYear() - offsetAntallAar
  );
  return new Date(yearsAgo);
};

const futureDate = () => {
  const newDate = new Date();
  const yearsFromNow = newDate.setFullYear(
    newDate.getFullYear() + offsetAntallAar
  );
  return new Date(yearsFromNow);
};

export { ControlledDateInput }