import { MetaFunction } from "react-router";
import { Heading, VStack } from "@navikt/ds-react";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { useIdFromUrl } from "~/utils/navigation";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { useArrangorflateUtbetaling } from "~/hooks/useArrangorflateUtbetaling";
import { useArrangorflateTilsagnTilUtbetaling } from "~/hooks/useArrangorflateTilsagnTilUtbetaling";
import { TilgjengeligeTilsagn } from "~/components/common/TilgjengeligeTilsagn";
import { BlokkeringerVarsler } from "~/components/common/BlokkeringerVarsler";
import { useUtbetalingWizard } from "~/hooks/useUtbetalingWizard";
import { StepFooter } from "~/components/utbetaling/StepFooter";

export const meta: MetaFunction = () => {
  return [
    { title: "Innsendingsinformasjon - Godkjenn innsending" },
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
  const wizard = useUtbetalingWizard(utbetaling);

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
          ]}
        />
        {tilsagn.length > 0 && <TilgjengeligeTilsagn tilsagn={tilsagn} />}
        <BlokkeringerVarsler
          blokkeringer={utbetaling.blokkeringer}
          advarsler={utbetaling.advarsler}
        />
      </VStack>
      <StepFooter wizard={wizard} />
    </>
  );
}
