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
  avbrytelse: TotrinnskontrollDto | null;
};

export function ToTrinnsAvbrytelseForklaring({ avbrytelse }: Props) {
  if (!avbrytelse) {
    return;
  }
  if (erBesluttet(avbrytelse)) {
    switch (avbrytelse.beslutning) {
      case TotrinnskontrollDtoBeslutning.RETURNERT:
        return (
          <AarsakerOgForklaring
            heading="Avbrytelse av utbetalingskrav ble avslått"
            tekster={[
              `${avbrytelse.besluttetAv.navn} avslo avbrytelsen ${formaterDato(avbrytelse.besluttetTidspunkt)}.`,
            ]}
            aarsaker={avbrytelse.besluttetAarsaker.map((aarsak) =>
              aarsakTilTekst(aarsak as UtbetalingStatusAarsak),
            )}
            forklaring={avbrytelse.besluttetBegrunnelse}
          />
        );
      case TotrinnskontrollDtoBeslutning.GODKJENT:
        return (
          <AarsakerOgForklaring
            heading="Avbrytelse av utbetalingskrav ble godkjent"
            tekster={[
              `${avbrytelse.behandletAv.navn || avbrytelse.behandletAv.agent} sendte kravet til avbrytelse den ${formaterDato(avbrytelse.behandletTidspunkt)}.`,
              `Godkjent av ${avbrytelse.besluttetAv.navn} `,
            ]}
            aarsaker={avbrytelse.behandletAarsaker.map((aarsak) =>
              aarsakTilTekst(aarsak as UtbetalingStatusAarsak),
            )}
            forklaring={avbrytelse.behandletBegrunnelse}
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
        `${avbrytelse.behandletAv.navn || avbrytelse.behandletAv.agent} sendte kravet til avbrytelse den ${formaterDato(avbrytelse.behandletTidspunkt)}.`,
      ]}
      aarsaker={avbrytelse.behandletAarsaker.map((aarsak) =>
        aarsakTilTekst(aarsak as UtbetalingStatusAarsak),
      )}
      forklaring={avbrytelse.behandletBegrunnelse}
    />
  );
}
