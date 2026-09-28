import {
  TotrinnskontrollDtoUtfall,
  UtbetalingDto,
  UtbetalingStatusAarsak,
  UtbetalingStatusDtoType,
} from "@tiltaksadministrasjon/api-client";
import { AarsakerOgForklaring } from "@/components/totrinnskontroll/AarsakerOgForklaring";
import { aarsakTilTekst } from "@/utils/Utils";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { erBesluttet, utledBehandletAvNavn, utledBesluttetAvNavn } from "@/utils/totrinnskontroll";

type Props = {
  utbetaling: UtbetalingDto;
};

export function TotrinnskontrollUtbetalingAvbrytelse({ utbetaling }: Props) {
  if (utbetaling.status.type === UtbetalingStatusDtoType.AVBRUTT_AV_NAV || !utbetaling.avbrytelse) {
    return null;
  }

  const avbrytelse = utbetaling.avbrytelse;
  if (erBesluttet(avbrytelse)) {
    switch (avbrytelse.beslutning.utfall) {
      case TotrinnskontrollDtoUtfall.RETURNERT:
        return (
          <AarsakerOgForklaring
            heading="Avbrytelse av utbetalingskrav ble avslått"
            tekster={[
              `${utledBesluttetAvNavn(avbrytelse)} avslo avbrytelsen ${formaterDato(avbrytelse.beslutning.tidspunkt)}.`,
            ]}
            aarsaker={avbrytelse.beslutning.aarsaker.map((aarsak) =>
              aarsakTilTekst(aarsak as UtbetalingStatusAarsak),
            )}
            forklaring={avbrytelse.beslutning.begrunnelse}
          />
        );
      case TotrinnskontrollDtoUtfall.GODKJENT:
        return (
          <AarsakerOgForklaring
            heading="Avbrytelse av utbetalingskrav ble godkjent"
            tekster={[
              `${utledBehandletAvNavn(avbrytelse)} sendte kravet til avbrytelse den ${formaterDato(avbrytelse.behandling.tidspunkt)}.`,
              `Godkjent av ${avbrytelse.beslutning.utfortAv.navn} `,
            ]}
            aarsaker={avbrytelse.behandling.aarsaker.map((aarsak) =>
              aarsakTilTekst(aarsak as UtbetalingStatusAarsak),
            )}
            forklaring={avbrytelse.behandling.begrunnelse}
          />
        );
      case TotrinnskontrollDtoUtfall.SATT_PA_VENT:
        return null;
    }
  }

  return (
    <AarsakerOgForklaring
      heading="Utbetalingskrav til avbrytelse"
      tekster={[
        `${utledBehandletAvNavn(avbrytelse)} sendte kravet til avbrytelse den ${formaterDato(avbrytelse.behandling.tidspunkt)}.`,
      ]}
      aarsaker={avbrytelse.behandling.aarsaker.map((aarsak) =>
        aarsakTilTekst(aarsak as UtbetalingStatusAarsak),
      )}
      forklaring={avbrytelse.behandling.begrunnelse}
    />
  );
}
