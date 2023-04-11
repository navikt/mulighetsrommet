import { UNSAFE_DatePicker, UNSAFE_useRangeDatepicker } from "@navikt/ds-react";
import { useController } from "react-hook-form";
import { formaterDato } from "../../utils/Utils";
import { inferredSchema } from "../avtaler/opprett/OpprettAvtaleContainer";

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
  const { field: fraDato } = useController<inferredSchema, "fraDato">({
    name: "fraDato",
  });
  const { field: tilDato } = useController<inferredSchema, "tilDato">({
    name: "tilDato",
  });
  const { datepickerProps, toInputProps, fromInputProps } =
    UNSAFE_useRangeDatepicker({
      onRangeChange: (val) => {
        fraDato.onChange(val?.from);
        tilDato.onChange(val?.to);
      },
    });

  return (
    <UNSAFE_DatePicker {...datepickerProps}>
      <div style={{ display: "flex", gap: "5rem" }}>
        <DatoFelt<T>
          {...fra}
          {...fromInputProps}
          ref={null}
          value={formaterDato(fraDato.value!!)}
        />
        <DatoFelt<T>
          {...til}
          {...toInputProps}
          ref={null}
          value={formaterDato(tilDato.value!!)}
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
