import { useSetStengtHosArrangor } from "@/api/gjennomforing/useSetStengtHosArrangor";
import { QueryKeys } from "@/api/QueryKeys";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { GjennomforingDto, SetStengtHosArrangorRequest, ValidationError } from "@mr/api-client-v2";
import { addDuration, subDuration } from "@mr/frontend-common/utils/date";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import { Alert, Box, Button, HStack, TextField, VStack } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { FormProvider, useForm } from "react-hook-form";

interface RegistrerStengtHosArrangorFormProps {
  gjennomforing: GjennomforingDto;
}

export function RegistrerStengtHosArrangorForm({
  gjennomforing,
}: RegistrerStengtHosArrangorFormProps) {
  const setStengtHosArrangor = useSetStengtHosArrangor(gjennomforing.id);
  const queryClient = useQueryClient();

  const form = useForm<SetStengtHosArrangorRequest>({});

  const { register, handleSubmit, formState, setError, getValues, setValue } = form;

  function onSubmit(data: SetStengtHosArrangorRequest) {
    setStengtHosArrangor.mutate(data, {
      onSuccess: async () => {
        form.reset();
        await queryClient.invalidateQueries({
          queryKey: QueryKeys.gjennomforing(gjennomforing.id),
          refetchType: "all",
        });
      },
      onValidationError: (error: ValidationError) => {
        error.errors.forEach((error) => {
          const name = jsonPointerToFieldPath(error.pointer) as keyof SetStengtHosArrangorRequest;
          setError(name, { type: "custom", message: error.detail });
        });
      },
    });
  }

  const minDate = subDuration(new Date(), { years: -1 });
  const maxDate = addDuration(new Date(), { years: 5 });

  return (
    <FormProvider {...form}>
      <form
        onSubmit={handleSubmit((values) => {
          onSubmit(values);
        })}
      >
        <VStack gap="2">
          <Box
            background="surface-subtle"
            borderColor="border-subtle"
            borderRadius="medium"
            borderWidth="1"
            padding="4"
          >
            <VStack gap="2">
              <HStack gap="4">
                <ControlledDateInput
                  label="Periode start"
                  defaultSelected={getValues("periodeStart")}
                  onChange={(val) => setValue("periodeStart", val)}
                  error={formState.errors.periodeStart?.message}
                  fromDate={minDate}
                  toDate={maxDate}
                />
                <ControlledDateInput
                  label="Periode slutt"
                  defaultSelected={getValues("periodeSlutt")}
                  onChange={(val) => setValue("periodeSlutt", val)}
                  error={formState.errors.periodeSlutt?.message}
                  fromDate={minDate}
                  toDate={maxDate}
                />
              </HStack>
              <TextField
                size="small"
                label="Beskrivelse"
                error={formState.errors.beskrivelse?.message as string}
                {...register("beskrivelse")}
              />
            </VStack>
          </Box>
          {setStengtHosArrangor.error && (
            <VStack className="my-5">
              <Alert inline variant="error">
                Klarte ikke lagre periode
              </Alert>
            </VStack>
          )}
          <Button
            className="self-start"
            size="small"
            icon={<FloppydiskIcon aria-hidden />}
            type="submit"
          >
            Lagre periode
          </Button>
        </VStack>
      </form>
    </FormProvider>
  );
}
