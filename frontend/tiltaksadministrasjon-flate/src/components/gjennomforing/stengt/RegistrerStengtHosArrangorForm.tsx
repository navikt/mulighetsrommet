import { useSetStengtHosArrangor } from "@/api/gjennomforing/useSetStengtHosArrangor";
import { SetStengtHosArrangorRequest, ValidationError } from "@tiltaksadministrasjon/api-client";
import { addDuration, subDuration } from "@mr/frontend-common/utils/date";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import { Alert, Box, Button, HStack, VStack } from "@navikt/ds-react";
import { FormProvider, useForm } from "react-hook-form";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { FormTextField } from "@/components/skjema/FormTextField";
import { applyValidationErrors } from "@/components/skjema/helpers";

interface RegistrerStengtHosArrangorFormProps {
  gjennomforingId: string;
}

export function RegistrerStengtHosArrangorForm({
  gjennomforingId,
}: RegistrerStengtHosArrangorFormProps) {
  const setStengtHosArrangor = useSetStengtHosArrangor(gjennomforingId);

  const form = useForm<SetStengtHosArrangorRequest>({});

  function onSubmit(data: SetStengtHosArrangorRequest) {
    setStengtHosArrangor.mutate(data, {
      onSuccess: () => {
        form.reset();
      },
      onValidationError: (error: ValidationError) => applyValidationErrors(form, error),
    });
  }

  const minDate = subDuration(new Date(), { years: 1 });
  const maxDate = addDuration(new Date(), { years: 5 });

  return (
    <FormProvider {...form}>
      <form
        onSubmit={form.handleSubmit((values) => {
          onSubmit(values);
        })}
      >
        <VStack gap="space-8">
          <Box
            background="neutral-soft"
            borderColor="neutral-subtle"
            borderRadius="4"
            borderWidth="1"
            padding="space-16"
          >
            <VStack gap="space-8">
              <HStack gap="space-16">
                <FormDateInput<SetStengtHosArrangorRequest>
                  name="periodeStart"
                  label="Periode start"
                  fromDate={minDate}
                  toDate={maxDate}
                />
                <FormDateInput<SetStengtHosArrangorRequest>
                  name="periodeSlutt"
                  label="Periode slutt"
                  fromDate={minDate}
                  toDate={maxDate}
                />
              </HStack>
              <FormTextField<SetStengtHosArrangorRequest> name="beskrivelse" label="Beskrivelse" />
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
