import { UNSAFE_DatePicker, UNSAFE_useRangeDatepicker } from "@navikt/ds-react";
import { useController } from "react-hook-form";
import { inferredSchema } from "../avtaler/OpprettAvtaleContainer";
import style from "./Datovelger.module.scss";
import { formaterDato } from "../../utils/Utils";

interface DatoProps {
  name: string;
  label: string;
  error?: string;
}

export function Datovelger<T>({
  fra,
  til,
}: {
  fra: DatoProps;
  til: DatoProps;
}) {
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

  const { datepickerProps, toInputProps, fromInputProps, setSelected } =
    UNSAFE_useRangeDatepicker({
      onRangeChange: (val) => {
        if (!val) return;
        startDato.onChange(val?.from);
        sluttDato.onChange(val?.to);
      },
      allowTwoDigitYear: true,
      inputFormat: "dd.MM.yyyy",
      fromDate: pastDate(),
      toDate: futureDate(),
      // defaultMonth: setSelected,
      // onValidate: setSelected,
    });

  return (
    <UNSAFE_DatePicker {...datepickerProps} dropdownCaption>
      <div className={style.datofelt}>
        <DatoFelt<T>
          {...fra}
          {...fromInputProps}
          ref={null}
          value={formaterDato(startDato.value!!)}
        />
        <DatoFelt<T>
          {...til}
          {...toInputProps}
          ref={null}
          value={formaterDato(sluttDato.value!!)}
        />
      </div>
    </UNSAFE_DatePicker>
  );
}

export function DatoFelt<T>({
  name,
  label,
  value,
  ...rest
}: { name: keyof T; label: string } & any) {
  return (
    <UNSAFE_DatePicker.Input
      {...rest}
      label={label}
      name={name}
      size="medium"
      defaultValue={value}
    />
  );
}
