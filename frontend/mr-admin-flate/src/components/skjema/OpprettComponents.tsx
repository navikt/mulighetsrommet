import {
  Checkbox,
  Select,
  Textarea,
  TextField,
  UNSAFE_DatePicker,
  UNSAFE_useRangeDatepicker,
} from "@navikt/ds-react";
import { FieldHookConfig, useField } from "formik";
import { useController } from "react-hook-form";
import { inferredSchema } from "../avtaler/opprett/OpprettAvtaleContainer";
import { formaterDato } from "../../utils/Utils";

export function Tekstfelt<T>({
  label,
  name,
  hjelpetekst,
  ...props
}: {
  name: keyof T;
  label: string;
  hjelpetekst?: string;
} & FieldHookConfig<any>) {
  const [field, meta] = useField({ name, ...props });
  return (
    <TextField
      description={hjelpetekst}
      size="small"
      label={label}
      {...field}
      error={meta.touched && meta.error}
    />
  );
}

export function TekstareaFelt<T>({
  label,
  name,
  hjelpetekst,
  size = "small",
  ...props
}: {
  name: keyof T;
  label: string;
  hjelpetekst?: string;
  size?: "small" | "medium";
} & FieldHookConfig<any>) {
  const [field, meta] = useField({ name, ...props });
  return (
    <Textarea
      description={hjelpetekst}
      size={size}
      label={label}
      {...field}
      error={meta.touched && meta.error}
    />
  );
}

export function SelectFelt<T>({
  label,
  name,
  defaultBlank = true,
  defaultBlankName = "",
  ...props
}: {
  name: keyof T;
  label: string;
  defaultBlank?: boolean;
  defaultBlankName?: string;
} & FieldHookConfig<any>) {
  const [field, meta] = useField({ name, ...props });
  return (
    <Select
      size="small"
      label={label}
      {...field}
      error={meta.touched && meta.error}
    >
      {defaultBlank ? <option value="">{defaultBlankName}</option> : null}
      {props.children}
    </Select>
  );
}

export function CheckboxFelt<T>(
  props: { name: keyof T } & FieldHookConfig<any>
) {
  const [field] = useField({ ...props, type: "checkbox" });

  return <Checkbox {...field}>{props.children}</Checkbox>;
}

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
        fraDato.onChange(val?.from ? formaterDato(val?.from) : undefined);
        tilDato.onChange(val?.to ? formaterDato(val?.to) : undefined);
      },
    });

  return (
    <UNSAFE_DatePicker {...datepickerProps}>
      <div style={{ display: "flex", gap: "5rem" }}>
        <DatoFelt<T> {...fra} {...fromInputProps} ref={null} />
        <DatoFelt<T> {...til} {...toInputProps} ref={null} />
      </div>
    </UNSAFE_DatePicker>
  );
}

export function DatoFelt<T>({
  name,
  label,
  ...rest
}: { name: keyof T; label: string } & FieldHookConfig<any> & any) {
  return (
    <UNSAFE_DatePicker.Input {...rest} label={label} name={name} size="small" />
  );
}
