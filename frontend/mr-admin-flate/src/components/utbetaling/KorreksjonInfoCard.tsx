import { InfoCard } from "@navikt/ds-react";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { MetadataHGrid } from "@mr/frontend-common/components/datadriven/Metadata";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { useUtbetaling } from "@/pages/gjennomforing/utbetaling/utbetalingPageLoader";

interface Props {
  utbetalingId: string;
}

export function KorreksjonInfoCard({ utbetalingId }: Props) {
  const { utbetaling } = useUtbetaling(utbetalingId);

  return (
    <InfoCard>
      <InfoCard.Header>
        <InfoCard.Title>Dette er en korreksjon for følgende utbetaling</InfoCard.Title>
      </InfoCard.Header>
      <InfoCard.Content>
        <MetadataHGrid
          label={utbetalingTekster.metadata.periode}
          value={formaterPeriode(utbetaling.periode)}
        />
        <MetadataHGrid
          label={utbetalingTekster.utbetalt.label}
          value={utbetaling.utbetalt ? formaterValutaBelop(utbetaling.utbetalt) : null}
        />
      </InfoCard.Content>
    </InfoCard>
  );
}
