import React from "react";
import {
  Checkbox,
  Select,
  TextField,
  UNSAFE_DatePicker,
  UNSAFE_useRangeDatepicker,
} from "@navikt/ds-react";
import { FieldHookConfig, useField } from "formik";
import { OpprettTiltakstypeSchema } from "./tiltakstyper/opprett-tiltakstyper/OpprettTiltakstypeSchemaValidation";
import { formaterDato } from "../utils/Utils";
import { z } from "zod";
import { OpprettTiltaksgjennomforingSchema } from "./tiltaksgjennomforinger/opprett-tiltaksgjennomforinger/OpprettTiltaksgjennomforingSchemaValidation";

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

export function Datovelger() {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [fraDatoField, fraDatoMeta, fraDatoHelper] = useField("fraDato");
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [tilDatoField, tilDatoMeta, tilDatoHelper] = useField("tilDato");
  const { datepickerProps, toInputProps, fromInputProps } =
    UNSAFE_useRangeDatepicker({
      onRangeChange: (val) => {
        fraDatoHelper.setValue(formaterDato(val?.from));
        tilDatoHelper.setValue(formaterDato(val?.to));
      },
    });

  return (
    <UNSAFE_DatePicker {...datepickerProps}>
      <div style={{ display: "flex", gap: "5rem" }}>
        <DatoFelt
          name="fraDato"
          label="Fra dato"
          {...fromInputProps}
          ref={null}
        />
        <DatoFelt
          name="tilDato"
          label="Til dato"
          {...toInputProps}
          ref={null}
        />
      </div>
    </UNSAFE_DatePicker>
  );
}

export function DatoFelt<T>({
  name,
  label,
  ...rest
}: { name: keyof T; label: string } & FieldHookConfig<any> & any) {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [_, meta] = useField({ name, ...rest });
  return (
    <UNSAFE_DatePicker.Input
      {...rest}
      label={label}
      name={name}
      error={meta.touched && meta.error}
    />
  );
}

export type OpprettTiltakstypeSchemaValues = z.infer<
  typeof OpprettTiltakstypeSchema
>;

export type OpprettTiltaksgjennomforingSchemaValues = z.infer<
  typeof OpprettTiltaksgjennomforingSchema
>;

export type OptionalTiltakstypeSchemaValues = Partial<
  z.infer<typeof OpprettTiltakstypeSchema>
>;
export type OptionalTiltaksgjennomforingSchemaValues = Partial<
  z.infer<typeof OpprettTiltaksgjennomforingSchema>
>;
