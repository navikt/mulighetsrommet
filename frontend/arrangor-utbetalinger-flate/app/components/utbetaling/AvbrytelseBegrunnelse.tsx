import { Avbrytelse } from "@arrangor-utbetalinger/api-client";
import { TotrinnsBegrunnelse } from "@mr/frontend-common";
import { MetadataFritekstfelt } from "@mr/frontend-common/components/datadriven/Metadata";
import { Heading } from "@navikt/ds-react";
import { Definition } from "../common/Definisjonsliste";
import { formaterDato } from "@mr/frontend-common/utils/date";

export function avbruttDato(avbrytelse: Avbrytelse | null): Definition[] {
  if (avbrytelse) {
    return [{ key: "Status endret", value: formaterDato(avbrytelse.avbruttDato) }];
  }
  return [];
}

function aarsakTilTekst(aarsak: string): string {
  switch (aarsak) {
    case "TILSAGN_GJORT_OPP":
      return "Tilsagnet er gjort opp";
    case "ANNET":
      return "Annet";
    default:
      return aarsak;
  }
}

interface AvbrytelseBegrunnelseProps {
  avbrytelse: Avbrytelse | null;
}

export function AvbrytelseBegrunnelse({ avbrytelse }: AvbrytelseBegrunnelseProps) {
  if (!avbrytelse) return null;

  const tittel = "Begrunnelse for avbrytelse";
  switch (avbrytelse.type) {
    case "AVBRUTT_AV_ARRANGOR":
      return (
        <>
          <Heading level="4" size="medium">
            {tittel}
          </Heading>
          <MetadataFritekstfelt label="Forklaring" value={avbrytelse.begrunnelse} />
        </>
      );
    case "AVBRUTT_AV_NAV":
      return (
        <TotrinnsBegrunnelse
          title={tittel}
          aarsaker={avbrytelse.aarsaker.map(aarsakTilTekst)}
          forklaring={avbrytelse.forklaring}
          headerSpacing={false}
          size="medium"
        />
      );
  }
}
