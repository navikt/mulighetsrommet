import {
  GjennomforingDto,
  OpprettUtbetalingRequest,
  PrismodellDto,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { Button, HStack } from "@navikt/ds-react";
import { FormProvider, useForm } from "react-hook-form";
import { useNavigate } from "react-router";
import { ArrangorBetalingsinformasjon } from "@/pages/gjennomforing/utbetaling/ArrangorBetalingsinformasjon";
import { ReactElement, useRef } from "react";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { FormGroup } from "@/components/skjema/FormGroup";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { useOpprettUtbetaling } from "@/api/utbetaling/mutations";

interface Props {
  gjennomforing: GjennomforingDto;
  prismodell: PrismodellDto;
  children: ReactElement;
}

export function UtbetalingForm({ gjennomforing, prismodell, children }: Props) {
  const navigate = useNavigate();

  const form = useForm<OpprettUtbetalingRequest>({
    resolver: async (values) => ({ values, errors: {} }),
  });

  const utbetalingId = useRef(window.crypto.randomUUID());
  const mutation = useOpprettUtbetaling(utbetalingId.current);
  function submit(data: OpprettUtbetalingRequest) {
    mutation.mutate(
      {
        ...data,
        kidNummer: data.kidNummer || null,
        gjennomforingId: gjennomforing.id,
        pris: { belop: data.pris?.belop ?? null, valuta: prismodell.valuta },
      },
      {
        onSuccess: () => {
          form.reset();
          navigate(`/gjennomforinger/${gjennomforing.id}/utbetalinger/${utbetalingId.current}`);
        },
        onValidationError: (error: ValidationError) => {
          error.errors.forEach((error) => {
            const name = jsonPointerToFieldPath(error.pointer) as keyof Omit<
              OpprettUtbetalingRequest,
              "gjennomforingId"
            >;
            form.setError(name, { type: "custom", message: error.detail });
          });
        },
      },
    );
  }

  return (
    <FormProvider {...form}>
      <form onSubmit={form.handleSubmit(submit)}>
        <TwoColumnGrid separator>
          {children}
          <FormGroup>
            <ArrangorBetalingsinformasjon arrangorId={gjennomforing.arrangor.id} />
          </FormGroup>
        </TwoColumnGrid>

        <HStack align={"start"} justify={"end"} gap="space-8">
          <Button
            size="small"
            variant="tertiary"
            onClick={() => navigate(`/gjennomforinger/${gjennomforing.id}/utbetalinger`)}
          >
            Avbryt
          </Button>
          <Button size="small" type="submit">
            Opprett
          </Button>
        </HStack>
      </form>
    </FormProvider>
  );
}
