import {
  GjennomforingDto,
  OpprettUtbetalingRequest,
  PrismodellDto,
} from "@tiltaksadministrasjon/api-client";
import { Button, HGrid, HStack } from "@navikt/ds-react";
import { addDuration } from "@mr/frontend-common/utils/date";
import { FormGroup } from "@/components/skjema/FormGroup";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { NumberInput } from "@/components/skjema/NumberInput";
import { FormProvider } from "react-hook-form";
import { useOpprettUtbetalingForm } from "@/pages/gjennomforing/utbetaling/useOpprettUtbetalingForm";
import { useNavigate } from "react-router";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { ArrangorBetalingsinformasjon } from "@/pages/gjennomforing/utbetaling/ArrangorBetalingsinformasjon";
import { FormTextarea } from "@/components/skjema/FormTextarea";

interface Props {
  gjennomforing: GjennomforingDto;
  prismodell: PrismodellDto;
}

export function OpprettUtbetalingKorreksjonForm({ gjennomforing, prismodell }: Props) {
  const navigate = useNavigate();

  const { form, onSubmit } = useOpprettUtbetalingForm({
    gjennomforingId: gjennomforing.id,
    pris: { belop: null, valuta: prismodell.valuta },
  });

  return (
    <FormProvider {...form}>
      <form onSubmit={onSubmit}>
        <TwoColumnGrid separator>
          <FormGroup>
            <HGrid columns={2}>
              <FormDateInput
                name="periodeStart"
                label="Periodestart"
                fromDate={new Date(gjennomforing.startDato)}
                toDate={addDuration(new Date(), { years: 5 })}
              />
              <FormDateInput
                name="periodeSlutt"
                label="Periodeslutt"
                fromDate={new Date(gjennomforing.startDato)}
                toDate={addDuration(new Date(), { years: 5 })}
              />
            </HGrid>
            <NumberInput<OpprettUtbetalingRequest>
              label={`Beløp (${prismodell.valuta})`}
              name="pris.belop"
            />
            <FormTextarea<OpprettUtbetalingRequest>
              label="Begrunnelse for utbetaling"
              name="beskrivelse"
            />
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
