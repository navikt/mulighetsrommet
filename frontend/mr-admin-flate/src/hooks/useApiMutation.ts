import { useMutation, UseMutationOptions, UseMutationResult } from "@tanstack/react-query";
import { ProblemDetail, ValidationError } from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";
import { isValidationError } from "@/utils/Utils";
import { useCallback, useEffect, useRef } from "react";

interface ApiMutateOptions<TData, TError, TVariables, TContext> {
  onSuccess?: (data: TData, vars: TVariables, ctx: TContext) => void;
  onError?: (error: TError, vars: TVariables, ctx: TContext | undefined) => void;
  onValidationError?: (error: ValidationError) => void;
}

export type ApiMutationResult<TData, TError, TVariables, TContext> = UseMutationResult<
  TData,
  TError,
  TVariables,
  TContext
> & {
  mutate: (
    variables: TVariables,
    options?: ApiMutateOptions<TData, TError, TVariables, TContext>,
  ) => void;
  mutateAsync: (
    variables: TVariables,
    options?: ApiMutateOptions<TData, TError, TVariables, TContext>,
  ) => Promise<TData>;
};

export function useApiMutation<
  TData,
  TError = ProblemDetail,
  TVariables = void,
  TContext = unknown,
>(
  options: UseMutationOptions<TData, TError, TVariables, TContext>,
): ApiMutationResult<TData, TError, TVariables, TContext> {
  const navigate = useNavigate();

  const { mutate: baseMutate, ...rest } = useMutation(options);

  // Cache latest options (for stable mutate reference)
  const latestOptionsRef = useRef(options);
  useEffect(() => {
    latestOptionsRef.current = options;
  }, [options]);

  const mutate = useCallback(
    (
      variables: TVariables,
      mutateOptions?: ApiMutateOptions<TData, TError, TVariables, TContext>,
    ) => {
      const currentOptions = latestOptionsRef.current;
      baseMutate(variables, {
        onSuccess: mutateOptions?.onSuccess ?? currentOptions.onSuccess,
        onError: (error, variables, context) => {
          if (isValidationError(error) && mutateOptions?.onValidationError) {
            mutateOptions.onValidationError(error);
          } else if (mutateOptions?.onError) {
            mutateOptions.onError(error, variables, context);
          } else {
            navigate("/error", { state: { problemDetail: error } });
          }
        },
      });
    },
    [baseMutate, navigate],
  );

  return {
    mutate,
    ...rest,
  };
}
