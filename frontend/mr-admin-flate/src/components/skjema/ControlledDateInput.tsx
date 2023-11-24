import { DatePicker, useDatepicker } from "@navikt/ds-react";
import { forwardRef } from "react";
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
}

export const ControlledDateInput = forwardRef(function ControlledDateInput(
  props: DateInputProps,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  _,
) {
  const { label, size, readOnly, format, fromDate, toDate, ...rest } = props;

  return (
    <div>
      <Controller
        name={label}
        {...rest}
        render={({ field: { onChange, value }, fieldState: { error } }) => {
          const { datepickerProps: startdatoProps, inputProps: startdatoInputProps } =
            useDatepicker({
              onDateChange: (val) => {
                if (val) {
                  if (format === "iso-string") {
                    onChange(formaterSomIsoDate(val));
                  } else {
                    onChange(val);
                  }
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
