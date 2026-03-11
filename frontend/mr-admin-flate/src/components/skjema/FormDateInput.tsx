import { Controller, useFormContext, type RegisterOptions } from "react-hook-form";
import { ControlledDateInput, ControlledDateInputProps } from "./ControlledDateInput";

interface FormDateInputProps extends Omit<
  ControlledDateInputProps,
  "onChange" | "defaultSelected"
> {
  name: string;
  rules?: RegisterOptions;
}

export function FormDateInput({ name, rules, size = "small", ...props }: FormDateInputProps) {
  const { control } = useFormContext();

  return (
    <Controller
      control={control}
      name={name}
      rules={rules}
      render={({ field, fieldState: { error } }) => (
        <ControlledDateInput
          {...props}
          size={size}
          error={error?.message}
          onChange={(dateString) => {
            field.onChange(dateString ? new Date(dateString) : undefined);
          }}
          defaultSelected={field.value ? field.value.toISOString().split("T")[0] : null}
        />
      )}
    />
  );
}
