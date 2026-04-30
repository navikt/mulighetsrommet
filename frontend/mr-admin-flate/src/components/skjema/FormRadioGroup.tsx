import { RadioGroup, Stack, type RadioGroupProps } from "@navikt/ds-react";
import { ReactNode } from "react";
import {
  useController,
  useFormContext,
  type FieldPath,
  type FieldValues,
  type RegisterOptions,
} from "react-hook-form";

type FormRadioGroupProps<TFieldValues extends FieldValues> = Omit<
  RadioGroupProps,
  "onChange" | "value" | "children"
> & {
  name: FieldPath<TFieldValues>;
  rules?: RegisterOptions<TFieldValues>;
  children: ReactNode;
  horisontal?: boolean;
};

export function FormRadioGroup<TFieldValues extends FieldValues>({
  name,
  rules,
  children,
  horisontal = false,
  ...props
}: FormRadioGroupProps<TFieldValues>) {
  const { control } = useFormContext<TFieldValues>();
  const { field, fieldState } = useController({ name, control, rules });

  return (
    <RadioGroup
      {...props}
      value={field.value}
      onChange={field.onChange}
      error={fieldState.error?.message}
    >
      <Stack
        gap="space-0 space-24"
        direction={horisontal ? { xs: "column", sm: "row" } : { xs: "column", sm: "column" }}
        wrap={false}
      >
        {children}
      </Stack>
    </RadioGroup>
  );
}
