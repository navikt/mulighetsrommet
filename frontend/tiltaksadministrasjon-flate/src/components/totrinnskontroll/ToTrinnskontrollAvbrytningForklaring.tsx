import {
  UtbetalingStatusAarsak,
  TotrinnskontrollDto,
  TotrinnskontrollDtoBeslutning,
} from "@tiltaksadministrasjon/api-client";
import { AarsakerOgForklaring } from "@/components/totrinnskontroll/AarsakerOgForklaring";
import { aarsakTilTekst } from "@/utils/Utils";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { erBesluttet } from "@/utils/totrinnskontroll";

type Props = {
  avbrytelse: TotrinnskontrollDto;
};

export function ToTrinnsAvbrytelseForklaring({ avbrytelse }: Props) {
  if (erBesluttet(avbrytelse)) {
    switch (avbrytelse.beslutning) {
      case TotrinnskontrollDtoBeslutning.RETURNERT:
        return (
          <AarsakerOgForklaring
            heading="Avbrytelse av utbetalingskrav ble avslått"
            tekster={[
              `${avbrytelse.besluttetAv.navn} avslo avbrytningen ${formaterDato(avbrytelse.besluttetTidspunkt)}.`,
            ]}
            aarsaker={avbrytelse.aarsaker.map((aarsak) =>
              aarsakTilTekst(aarsak as UtbetalingStatusAarsak),
            )}
            forklaring={avbrytelse.forklaring}
          />
        );
      case TotrinnskontrollDtoBeslutning.GODKJENT:
        return (
          <AarsakerOgForklaring
            heading="Avbrytelse av utbetalingskrav ble godkjent"
            tekster={[
              `${avbrytelse.behandletAv.navn} sendte kravet til avbrytning den ${formaterDato(avbrytelse.behandletTidspunkt)}.`,
              `Godkjent av ${avbrytelse.besluttetAv.navn} `,
            ]}
            aarsaker={avbrytelse.aarsaker.map((aarsak) =>
              aarsakTilTekst(aarsak as UtbetalingStatusAarsak),
            )}
            forklaring={avbrytelse.forklaring}
          />
        );
      case TotrinnskontrollDtoBeslutning.SATT_PA_VENT:
        return null;
    }
  }
  return (
    <AarsakerOgForklaring
      heading="Utbetalingskrav til avbrytelse"
      tekster={[
        `${avbrytelse.behandletAv.navn} sendte kravet til avbrytning den ${formaterDato(avbrytelse.behandletTidspunkt)}.`,
      ]}
      aarsaker={avbrytelse.aarsaker.map((aarsak) =>
        aarsakTilTekst(aarsak as UtbetalingStatusAarsak),
      )}
      forklaring={avbrytelse.forklaring}
    />
  );
}
