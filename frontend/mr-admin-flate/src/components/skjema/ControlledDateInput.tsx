import { formaterDato } from "../../utils/Utils";
import styles from "./ControlledDateInput.module.scss";
import { forwardRef } from "react";
import { Controller } from "react-hook-form";
import { DatePicker, useDatepicker } from "@navikt/ds-react";

export interface DateInputProps {
  label: string;
  readOnly?: boolean;
  fromDate: Date;
  toDate: Date;
  size?: "small" | "medium";
}

export const ControlledDateInput = forwardRef(function ControlledDateInput(
  props: DateInputProps,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  _,
) {
  const { label, size, readOnly, fromDate, toDate, ...rest } = props;

  return (
    <div>
      <Controller
        name={label}
        {...rest}
        render={({ field: { onChange, value }, fieldState: { error } }) => {
          const {
            datepickerProps: startdatoProps,
            inputProps: startdatoInputProps,
            selectedDay: selectedStartdato,
          } = useDatepicker({
            onDateChange: (val) => {
              if (val) {
                onChange(val);
              } else {
                onChange(null);
              }
            },
            allowTwoDigitYear: true,
            inputFormat: "dd.MM.yyyy",
            fromDate,
            toDate,
            defaultSelected: value ? new Date(value) : undefined,
          });
          return (
            <DatePicker {...startdatoProps} dropdownCaption>
              <DatoFelt
                size={size}
                label={label}
                {...rest}
                {...startdatoInputProps}
                error={error?.message}
                value={selectedStartdato ? formaterDato(selectedStartdato) : ""}
                readOnly={readOnly}
              />
            </DatePicker>
          );
        }}
      />
    </div>
  );
});

const DatoFelt = forwardRef(function DatoFeltInput(props: any, ref: any) {
  const { name, label, size, ...rest } = props;
  return (
    <DatePicker.Input
      {...rest}
      label={label}
      name={name}
      size={size}
      ref={ref}
      className={styles.dato_input}
    />
  );
});
