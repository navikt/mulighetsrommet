import { Textarea, TextareaProps } from "@navikt/ds-react";
import { FieldValues, get, Path, useFormContext } from "react-hook-form";

interface Props<T extends FieldValues> extends Omit<TextareaProps, "error"> {
  name: Path<T>;
}

export function TextareaInput<T extends FieldValues>({ name, ...rest }: Props<T>) {
  const {
    register,
    formState: { errors },
  } = useFormContext<T>();
  const error = get(errors, name)?.message as string | undefined;
  return <Textarea size="small" {...rest} {...register(name)} error={error} />;
}
