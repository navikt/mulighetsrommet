import { Link as ReactRouterLink, MetaFunction } from "react-router";
import { Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { pathTo, useIdFromUrl, useOrgnrFromUrl } from "~/utils/navigation";
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
  const orgnr = useOrgnrFromUrl();

  const { data: utbetaling } = useArrangorflateUtbetaling(id);
  const { data: tilsagn } = useArrangorflateTilsagnTilUtbetaling(id);

  const harTilsagn = tilsagn.length > 0;

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
        {harTilsagn && (
          <HStack gap="space-16">
            <Button as={ReactRouterLink} type="button" variant="tertiary" to={pathTo.utbetalinger}>
              Avbryt
            </Button>
            <Button
              as={ReactRouterLink}
              aria-label="Neste"
              to={pathTo.beregning(orgnr, utbetaling.id)}
            >
              Neste
            </Button>
          </HStack>
        )}
      </VStack>
    </>
  );
}
