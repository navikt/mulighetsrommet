import { zodResolver } from "@hookform/resolvers/zod";
import { ProblemDetail, ValidationError } from "@tiltaksadministrasjon/api-client";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { DefaultValues, FieldValues, Path, Resolver, useForm } from "react-hook-form";
import { ZodType } from "zod";
import { ApiMutationResult } from "./useApiMutation";

interface UseEditFormOptions<TFormValues extends FieldValues, TRequest> {
  schema: ZodType;
  defaultValues: DefaultValues<TFormValues>;
  mutation: ApiMutationResult<unknown, ProblemDetail, TRequest, any>;
  toRequest: (data: TFormValues) => TRequest;
  onSuccess: () => void;
  mapErrorFieldName?: (fieldPath: string) => string;
}

export function useEditForm<TFormValues extends FieldValues, TRequest>({
  schema,
  defaultValues,
  mutation,
  toRequest,
  onSuccess,
  mapErrorFieldName,
}: UseEditFormOptions<TFormValues, TRequest>) {
  const methods = useForm<TFormValues>({
    resolver: zodResolver(schema as any) as Resolver<TFormValues>,
    defaultValues,
  });

  function handleValidationError(validation: ValidationError) {
    if (!mapErrorFieldName) {
      applyValidationErrors(methods, validation);
      return;
    }
    validation.errors.forEach((error) => {
      const name = mapErrorFieldName(jsonPointerToFieldPath(error.pointer));
      methods.setError(name as Path<TFormValues>, { type: "custom", message: error.detail });
    });
  }

  const onSubmit = methods.handleSubmit((data) => {
    mutation.mutate(toRequest(data), {
      onValidationError: handleValidationError,
      onSuccess,
    });
  });

  return { methods, onSubmit };
}
