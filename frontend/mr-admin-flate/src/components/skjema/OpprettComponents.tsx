import { UNSAFE_DatePicker, UNSAFE_useRangeDatepicker } from "@navikt/ds-react";
import { useController } from "react-hook-form";
import { formaterDato } from "../../utils/Utils";
import { inferredSchema } from "../avtaler/OpprettAvtaleContainer";
import "./OpprettComponents.module.scss";

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
  const { datepickerProps, toInputProps, fromInputProps } =
    UNSAFE_useRangeDatepicker({
      onRangeChange: (val) => {
        startDato.onChange(val?.from);
        sluttDato.onChange(val?.to);
      },
    });

  return (
    <UNSAFE_DatePicker {...datepickerProps}>
      <div style={{ display: "flex", gap: "5rem" }}>
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
      value={value}
    />
  );
}
