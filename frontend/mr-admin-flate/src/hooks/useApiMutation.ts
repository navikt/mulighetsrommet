import { useMutation, UseMutationOptions, UseMutationResult } from "@tanstack/react-query";
import { ProblemDetail, ValidationError } from "@mr/api-client-v2";
import { useNavigate } from "react-router";
import { isValidationError } from "@/utils/Utils";

interface ApiMutateOptions<TData, TError, TVariables, TContext> {
  onSuccess?: (data: TData, vars: TVariables, ctx: TContext) => void;
  onError?: (error: TError, vars: TVariables, ctx: TContext | undefined) => void;
  onValidationError?: (error: ValidationError) => void;
}

export function useApiMutation<
  TData,
  TError = ProblemDetail,
  TVariables = void,
  TContext = unknown,
>(
  options: UseMutationOptions<TData, TError, TVariables, TContext>,
): UseMutationResult<TData, TError, TVariables, TContext> & {
  mutate: (
    variables: TVariables,
    options?: ApiMutateOptions<TData, TError, TVariables, TContext>,
  ) => void;
  mutateAsync: (
    variables: TVariables,
    options?: ApiMutateOptions<TData, TError, TVariables, TContext>,
  ) => Promise<TData>;
} {
  const navigate = useNavigate();

  const mutation = useMutation(options);

  const safeMutate = (
    variables: TVariables,
    mutateOptions?: ApiMutateOptions<TData, TError, TVariables, TContext>,
  ) => {
    mutation.mutate(variables, {
      onSuccess: mutateOptions?.onSuccess ?? options.onSuccess,
      onError: (error, variables, context) => {
        if (isValidationError(error) && mutateOptions?.onValidationError) {
          mutateOptions.onValidationError(error);
        } else if (mutateOptions?.onError) {
          mutateOptions?.onError?.(error, variables, context);
        } else {
          navigate("/error", { state: { problemDetail: error } });
        }
      },
    });
  };

  return {
    ...mutation,
    mutate: safeMutate,
  };
}
