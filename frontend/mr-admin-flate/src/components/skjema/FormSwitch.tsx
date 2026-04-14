import { Switch, type SwitchProps } from "@navikt/ds-react";
import {
  useController,
  useFormContext,
  type FieldPath,
  type FieldValues,
  type RegisterOptions,
} from "react-hook-form";

type FormSwitchProps<TFieldValues extends FieldValues> = Omit<
  SwitchProps,
  "checked" | "onChange" | "name"
> & {
  name: FieldPath<TFieldValues>;
  rules?: RegisterOptions<TFieldValues>;
};

export function FormSwitch<TFieldValues extends FieldValues>({
  name,
  rules,
  ...props
}: FormSwitchProps<TFieldValues>) {
  const { control } = useFormContext<TFieldValues>();
  const { field } = useController({ name, control, rules });

  return (
    <Switch
      {...props}
      name={field.name}
      ref={field.ref}
      checked={!!field.value}
      onChange={(e) => field.onChange(e.target.checked)}
      onBlur={field.onBlur}
    />
  );
}
