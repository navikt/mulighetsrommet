import {
  Controller,
  FieldPath,
  type FieldValues,
  type RegisterOptions,
  useFormContext,
} from "react-hook-form";
import { ControlledDateInput, ControlledDateInputProps } from "./ControlledDateInput";
import { yyyyMMddFormatting } from "@mr/frontend-common/utils/date";

interface FormDateInputProps<TFieldValues extends FieldValues> extends Omit<
  ControlledDateInputProps,
  "onChange" | "defaultSelected"
> {
  name: FieldPath<TFieldValues>;
  rules?: RegisterOptions<TFieldValues>;
}

export function FormDateInput<TFieldValues extends FieldValues>({
  name,
  rules,
  size = "small",
  ...props
}: FormDateInputProps<TFieldValues>) {
  const { control } = useFormContext<TFieldValues>();

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
