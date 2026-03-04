import { TextField, TextFieldProps } from "@navikt/ds-react";
import { FieldValues, get, Path, useFormContext } from "react-hook-form";

interface Props<T extends FieldValues> extends Omit<TextFieldProps, "error"> {
  name: Path<T>;
}

export function TextInput<T extends FieldValues>({ name, ...rest }: Props<T>) {
  const {
    register,
    formState: { errors },
  } = useFormContext<T>();
  const error = get(errors, name)?.message as string | undefined;
  return <TextField size="small" {...rest} {...register(name)} error={error} />;
}
