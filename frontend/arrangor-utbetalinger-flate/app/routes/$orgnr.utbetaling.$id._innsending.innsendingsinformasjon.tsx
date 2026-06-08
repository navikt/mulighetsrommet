import { MetaFunction } from "react-router";
import { Heading, VStack } from "@navikt/ds-react";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { useIdFromUrl } from "~/utils/navigation";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { useArrangorflateUtbetaling } from "~/hooks/useArrangorflateUtbetaling";
import { useArrangorflateTilsagnTilUtbetaling } from "~/hooks/useArrangorflateTilsagnTilUtbetaling";
import { TilgjengeligeTilsagn } from "~/components/common/TilgjengeligeTilsagn";

export const meta: MetaFunction = () => {
  return [
    { title: "Steg 1 av 3: Innsendingsinformasjon - Godkjenn innsending" },
    {
      name: "description",
      content: "Grunnleggende informasjon om innsendingen",
    },
  ];
};

export default function TilsagnDetaljerPage() {
  const id = useIdFromUrl();

  const { data: utbetaling } = useArrangorflateUtbetaling(id);
  const { data: tilsagn } = useArrangorflateTilsagnTilUtbetaling(id);

  return (
    <>
      <Heading level="2" spacing size="large">
        Innsendingsinformasjon
      </Heading>
      <VStack gap="space-16">
        <Definisjonsliste
          definitions={[
            {
              key: "Arrangør",
              value: `${utbetaling.arrangor.navn} - ${utbetaling.arrangor.organisasjonsnummer}`,
            },
            {
              key: "Tiltaksnavn",
              value: `${utbetaling.gjennomforing.navn} (${utbetaling.gjennomforing.lopenummer})`,
            },
            { key: "Tiltakstype", value: utbetaling.tiltakstype.navn },
            { key: "Utbetalingsperiode", value: formaterPeriode(utbetaling.periode) },
            {
              key: "Frist for innsending",
              value: "Kravet må sendes inn senest to måneder etter at tilsagnsperioden går ut.",
            },
          ]}
        />
        <TilgjengeligeTilsagn tilsagn={tilsagn} />
      </VStack>
    </>
  );
}
