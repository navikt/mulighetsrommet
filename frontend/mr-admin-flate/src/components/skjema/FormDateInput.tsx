import { Controller, type RegisterOptions, useFormContext } from "react-hook-form";
import { ControlledDateInput, ControlledDateInputProps } from "./ControlledDateInput";
import { yyyyMMddFormatting } from "@mr/frontend-common/utils/date";

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
            field.onChange(dateString);
          }}
          defaultSelected={yyyyMMddFormatting(field.value) || null}
        />
      )}
    />
  );
}
