import { MutateOptions, useMutation, UseMutationOptions, UseMutationResult } from "@tanstack/react-query";
import { ProblemDetail, ValidationError } from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";
import { isValidationError } from "@/utils/Utils";
import { useCallback, useEffect, useRef } from "react";

interface OnValidationErrorOption {
  onValidationError?: (error: ValidationError) => void;
}

type ApiMutateOptions<TData, TError, TVariables, TContext> = Pick<
  MutateOptions<TData, TError, TVariables, TContext>,
  "onSuccess" | "onError"
> &
  OnValidationErrorOption;

export type ApiMutationResult<TData, TError, TVariables, TOnMutateResult> = UseMutationResult<
  TData,
  TError,
  TVariables,
  TOnMutateResult
> & {
  mutate: (
    variables: TVariables,
    options?: ApiMutateOptions<TData, TError, TVariables, TOnMutateResult>,
  ) => void;
};

export function useApiMutation<
  TData,
  TError = ProblemDetail,
  TVariables = void,
  TOnMutateResult = unknown,
>(
  options: UseMutationOptions<TData, TError, TVariables, TOnMutateResult>,
): ApiMutationResult<TData, TError, TVariables, TOnMutateResult> {
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
      mutateOptions?: ApiMutateOptions<TData, TError, TVariables, TOnMutateResult>,
    ) => {
      const currentOptions = latestOptionsRef.current;
      baseMutate(variables, {
        onSuccess: (data, variables, onMutateResult, context) => {
          if (mutateOptions?.onSuccess) {
            mutateOptions.onSuccess(data, variables, onMutateResult, context);
          } else if (currentOptions.onSuccess && onMutateResult) {
            currentOptions.onSuccess(data, variables, onMutateResult, context);
          }
        },
        onError: (error, variables, onMutateResult, context) => {
          if (isValidationError(error) && mutateOptions?.onValidationError) {
            mutateOptions.onValidationError(error);
          } else if (mutateOptions?.onError) {
            mutateOptions.onError(error, variables, onMutateResult, context);
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
