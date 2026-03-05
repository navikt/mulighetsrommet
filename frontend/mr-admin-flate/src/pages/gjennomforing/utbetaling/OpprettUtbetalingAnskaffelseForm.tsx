import {
  GjennomforingDto,
  OpprettUtbetalingRequest,
  PrismodellDto,
} from "@tiltaksadministrasjon/api-client";
import { Button, HGrid, HStack } from "@navikt/ds-react";
import { addDuration } from "@mr/frontend-common/utils/date";
import { FormGroup } from "@/components/skjema/FormGroup";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { NumberInput } from "@/components/skjema/NumberInput";
import { TextInput } from "@/components/skjema/TextInput";
import { TextareaInput } from "@/components/skjema/TextareaInput";
import { FormProvider } from "react-hook-form";
import { useOpprettUtbetalingForm } from "@/pages/gjennomforing/utbetaling/useOpprettUtbetalingForm";
import { useNavigate } from "react-router";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { ArrangorBetalingsinformasjon } from "@/pages/gjennomforing/utbetaling/ArrangorBetalingsinformasjon";

interface Props {
  gjennomforing: GjennomforingDto;
  prismodell: PrismodellDto;
}

export function OpprettUtbetalingAnskaffelseForm({ gjennomforing, prismodell }: Props) {
  const navigate = useNavigate();

  const { form, onSubmit } = useOpprettUtbetalingForm({
    gjennomforingId: gjennomforing.id,
    pris: { belop: null, valuta: prismodell.valuta },
  });

  const {
    formState: { errors },
    clearErrors,
    setValue,
    getValues,
  } = form;

  return (
    <FormProvider {...form}>
      <form onSubmit={onSubmit}>
        <TwoColumnGrid separator>
          <FormGroup>
            <HGrid columns={2}>
              <ControlledDateInput
                label="Periodestart"
                fromDate={new Date(gjennomforing.startDato)}
                toDate={addDuration(new Date(), { years: 5 })}
                onChange={(val) => setValue("periodeStart", val)}
                defaultSelected={getValues("periodeStart")}
                clearErrors={() => clearErrors("periodeStart")}
                error={errors.periodeStart?.message}
              />
              <ControlledDateInput
                label="Periodeslutt"
                fromDate={new Date(gjennomforing.startDato)}
                toDate={addDuration(new Date(), { years: 5 })}
                onChange={(val) => setValue("periodeSlutt", val)}
                defaultSelected={getValues("periodeSlutt")}
                clearErrors={() => clearErrors("periodeSlutt")}
                error={errors.periodeSlutt?.message}
              />
            </HGrid>
            <NumberInput<OpprettUtbetalingRequest>
              label={`Beløp (${prismodell.valuta})`}
              name="pris.belop"
            />
            <TextInput<OpprettUtbetalingRequest>
              label="Journalpost-ID i Gosys"
              name="journalpostId"
            />
            <TextareaInput<OpprettUtbetalingRequest> label="Kommentar" name="beskrivelse" />
          </FormGroup>
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
