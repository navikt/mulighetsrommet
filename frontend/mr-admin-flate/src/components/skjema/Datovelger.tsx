import { DatePicker, useDatepicker} from "@navikt/ds-react";
import { useController } from "react-hook-form";
import { formaterDato } from "../../utils/Utils";
import styles from "./Datovelger.module.scss";
import { forwardRef } from "react";
import { inferredSchema } from "../avtaler/AvtaleSchema";

interface DatoProps {
  name: string;
  readOnly?: boolean;
  label: string;
  error?: string;
}

export function Datovelger({ fra, til }: { fra: DatoProps; til: DatoProps }) {
  const { field: startDato } = useController<inferredSchema, "startDato">({
    name: "startDato",
  });
  const { field: sluttDato } = useController<inferredSchema, "sluttDato">({
    name: "sluttDato",
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

  const {
    datepickerProps: startdatoProps,
    inputProps: startdatoInputProps,
    selectedDay: selectedStartdato,
  } = useDatepicker({
    onDateChange: (val) => {
      if (val) {
        startDato.onChange(val);
      } else {
        startDato.onChange(null);
      }
    },
    allowTwoDigitYear: true,
    inputFormat: "dd.MM.yyyy",
    fromDate: pastDate(),
    toDate: futureDate(),
    defaultSelected: startDato.value ? new Date(startDato.value) : undefined,
  });

  const {
    datepickerProps: sluttdatoProps,
    inputProps: sluttdatoInputProps,
    selectedDay: selectedSluttdato,
  } = useDatepicker({
    onDateChange: (val) => {
      if (val) {
        sluttDato.onChange(val);
      } else {
        sluttDato.onChange(null);
      }
    },
    allowTwoDigitYear: true,
    inputFormat: "dd.MM.yyyy",
    fromDate: pastDate(),
    toDate: futureDate(),
    defaultSelected: sluttDato.value ? new Date(sluttDato.value) : undefined,
  });

  return (
    <div className={styles.dato_container}>
      <DatePicker {...startdatoProps} dropdownCaption>
        <DatoFelt
          {...fra}
          {...startdatoInputProps}
          value={
            selectedStartdato ? formaterDato(selectedStartdato) : undefined
          }
        />
      </DatePicker>
      <DatePicker {...sluttdatoProps} dropdownCaption>
        <DatoFelt
          {...til}
          {...sluttdatoInputProps}
          value={
            selectedSluttdato ? formaterDato(selectedSluttdato) : undefined
          }
        />
      </DatePicker>
    </div>
  );
}

const DatoFelt = forwardRef(function DatoFeltInput(props: any, ref: any) {
  const { name, label, ...rest } = props;
  return (
    <DatePicker.Input
      {...rest}
      label={label}
      name={name}
      size="medium"
      ref={ref}
      disabled={rest.readOnly}
      className={styles.dato_input}
    />
  );
});
