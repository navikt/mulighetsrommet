import {
  TilsagnStatusAarsak,
  TilskuddBehandlingStatusAarsak,
  TotrinnskontrollDto,
} from "@tiltaksadministrasjon/api-client";
import { AarsakerOgForklaring } from "@/components/totrinnskontroll/AarsakerOgForklaring";
import { aarsakTilTekst } from "@/utils/Utils";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { erBesluttet, erReturnert, utledBesluttetAvNavn } from "@/utils/totrinnskontroll";

type Props = {
  heading: string;
  opprettelse: TotrinnskontrollDto;
};

export function TotrinnskontrollReturnert({ heading, opprettelse }: Props) {
  if (!erBesluttet(opprettelse) || !erReturnert(opprettelse)) {
    return null;
  }

  return (
    <AarsakerOgForklaring
      heading={heading}
      tekster={[
        `${utledBesluttetAvNavn(opprettelse)} returnerte den ${formaterDato(opprettelse.beslutning.tidspunkt)}.`,
      ]}
      aarsaker={opprettelse.beslutning.aarsaker.map((aarsak) =>
        aarsakTilTekst(aarsak as TilsagnStatusAarsak | TilskuddBehandlingStatusAarsak),
      )}
      forklaring={opprettelse.beslutning.begrunnelse}
    />
  );
}
