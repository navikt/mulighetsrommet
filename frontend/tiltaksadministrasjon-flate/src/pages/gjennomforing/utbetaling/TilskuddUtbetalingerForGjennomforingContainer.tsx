import { Alert, BodyShort, HStack, VStack } from "@navikt/ds-react";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useTilskuddUtbetalingerByGjennomforing } from "@/api/utbetaling/useTilskuddUtbetalingerByGjennomforing";
import { TilskuddUtbetalingTable } from "@/components/utbetaling/TilskuddUtbetalingTable";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { Valuta } from "@tiltaksadministrasjon/api-client";

export function TilskuddUtbetalingerForGjennomforingContainer() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: utbetalinger } = useTilskuddUtbetalingerByGjennomforing(gjennomforingId);

  return (
    <>
      {utbetalinger.length > 0 ? (
        <VStack gap="space-8">
          <TilskuddUtbetalingTable gjennomforingId={gjennomforingId} utbetalinger={utbetalinger} />
          <HStack gap="space-8">
            <BodyShort weight="semibold">Totalt beløp:</BodyShort>
            <BodyShort weight="semibold">
              {formaterValutaBelop({
                belop: utbetalinger.reduce((acc, t) => (t.belopUtbetalt?.belop ?? 0) + acc, 0),
                valuta: Valuta.NOK,
              })}
            </BodyShort>
          </HStack>
        </VStack>
      ) : (
        <Alert style={{ marginTop: "1rem" }} variant="info">
          Det finnes ingen utbetalinger for dette tiltaket
        </Alert>
      )}
    </>
  );
}
