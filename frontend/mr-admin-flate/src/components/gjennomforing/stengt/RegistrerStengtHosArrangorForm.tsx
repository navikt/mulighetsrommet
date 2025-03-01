import { GjennomforingDto, ProblemDetail, SetStengtHosArrangorRequest } from "@mr/api-client-v2";
import { Alert, Button, HGrid, TextField, VStack } from "@navikt/ds-react";
import { FormProvider, useForm } from "react-hook-form";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { addYear } from "@/utils/Utils";
import { useSetStengtHosArrangor } from "@/api/gjennomforing/useSetStengtHosArrangor";
import { isValidationError, jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { useRevalidator } from "react-router";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import { FormGroup } from "@/components/skjema/FormGroup";
import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";

interface RegistrerStengtHosArrangorFormProps {
  gjennomforing: GjennomforingDto;
}

export function RegistrerStengtHosArrangorForm({
  gjennomforing,
}: RegistrerStengtHosArrangorFormProps) {
  const setStengtHosArrangor = useSetStengtHosArrangor(gjennomforing.id);
  const revalidator = useRevalidator();
  const queryClient = useQueryClient();

  const form = useForm<SetStengtHosArrangorRequest>({});

  const { register, handleSubmit, formState, setError, control } = form;

  function onSubmit(data: SetStengtHosArrangorRequest) {
    setStengtHosArrangor.mutate(
      {
        beskrivelse: data.beskrivelse,
        periodeStart: data.periodeStart || undefined,
        periodeSlutt: data.periodeSlutt || undefined,
      },
      {
        onSuccess: async () => {
          form.reset();
          await queryClient.invalidateQueries({
            queryKey: QueryKeys.gjennomforing(gjennomforing.id),
            refetchType: "all",
          });
          revalidator.revalidate();
        },
        onError: (error: ProblemDetail) => {
          if (isValidationError(error)) {
            error.errors.forEach((error) => {
              const name = jsonPointerToFieldPath(
                error.pointer,
              ) as keyof SetStengtHosArrangorRequest;
              setError(name, { type: "custom", message: error.detail });
            });
          }
        },
      },
    );
  }

  const minDate = addYear(new Date(), -1);
  const maxDate = addYear(new Date(), 5);

  return (
    <FormProvider {...form}>
      <form
        onSubmit={handleSubmit((values) => {
          onSubmit(values);
        })}
      >
        <FormGroup>
          <HGrid columns={2}>
            <ControlledDateInput
              label="Periode start"
              fromDate={minDate}
              toDate={maxDate}
              format="iso-string"
              size="small"
              {...register("periodeStart")}
              control={control}
            />
            <ControlledDateInput
              label="Periode slutt"
              fromDate={minDate}
              toDate={maxDate}
              format="iso-string"
              size="small"
              {...register("periodeSlutt")}
              control={control}
            />
          </HGrid>
          <HGrid columns={1}>
            <TextField
              size="small"
              label="Beskrivelse"
              style={{ width: "180px" }}
              error={formState.errors.beskrivelse?.message as string}
              {...register("beskrivelse")}
            />
          </HGrid>
        </FormGroup>

        {setStengtHosArrangor.error && (
          <VStack className="my-5">
            <Alert inline variant="error">
              Klarte ikke lagre periode
            </Alert>
          </VStack>
        )}

        <Button size="small" icon={<FloppydiskIcon aria-hidden />} type="submit">
          Lagre periode
        </Button>
      </form>
    </FormProvider>
  );
}
