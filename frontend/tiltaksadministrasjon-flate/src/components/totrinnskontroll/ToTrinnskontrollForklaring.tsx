import {
  TilsagnStatusAarsak,
  TilskuddBehandlingStatusAarsak,
  TotrinnskontrollDto,
} from "@tiltaksadministrasjon/api-client";
import { AarsakerOgForklaring } from "@/components/totrinnskontroll/AarsakerOgForklaring";
import { aarsakTilTekst } from "@/utils/Utils";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { erReturnert, erBesluttet } from "@/utils/totrinnskontroll";

type Props = {
  heading: string;
  kontroll: TotrinnskontrollDto;
};

export function ToTrinnskontrollForklaring({ heading, kontroll }: Props) {
  if (!erBesluttet(kontroll) || !erReturnert(kontroll)) {
    return null;
  }

  return (
    <AarsakerOgForklaring
      heading={heading}
      tekster={[
        `${kontroll.besluttetAv.navn} returnerte den ${formaterDato(kontroll.besluttetTidspunkt)}.`,
      ]}
      aarsaker={kontroll.aarsaker.map((aarsak) =>
        aarsakTilTekst(aarsak as TilsagnStatusAarsak | TilskuddBehandlingStatusAarsak),
      )}
      forklaring={kontroll.forklaring}
    />
  );
}
