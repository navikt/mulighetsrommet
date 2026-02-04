import { Link as ReactRouterLink, MetaFunction, useParams } from "react-router";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { BodyShort, Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { UtbetalingManglendeTilsagnAlert } from "~/components/utbetaling/UtbetalingManglendeTilsagnAlert";
import { pathTo, useOrgnrFromUrl } from "~/utils/navigation";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { useArrangorflateUtbetaling } from "~/hooks/useArrangorflateUtbetaling";
import { useArrangorflateTilsagnTilUtbetaling } from "~/hooks/useArrangorflateTilsagnTilUtbetaling";

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
  const { id } = useParams();
  const orgnr = useOrgnrFromUrl();

  const { data: utbetaling } = useArrangorflateUtbetaling(id!);
  const { data: tilsagn } = useArrangorflateTilsagnTilUtbetaling(id!);

  const harTilsagn = tilsagn.length > 0;

  return (
    <>
      <Heading level="2" spacing size="large">
        Innsendingsinformasjon
      </Heading>
      <VStack gap="4">
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
        <Heading level="3" size="medium">
          Tilgjengelige tilsagn
        </Heading>
        <BodyShort size="small" textColor="subtle">
          Under vises informasjon om antatt forbruk.
          <br />
          Hva som blir utbetalt avhenger imidlertid av faktisk forbruk i perioden.
        </BodyShort>
        {!harTilsagn && <UtbetalingManglendeTilsagnAlert />}
        {tilsagn.map((t) => (
          <TilsagnDetaljer key={t.bestillingsnummer} tilsagn={t} headingLevel="4" minimal />
        ))}
        {harTilsagn && (
          <HStack gap="4" className="mt-4">
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
