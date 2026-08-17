import { Heading, HGrid, InlineMessage, Box } from "@navikt/ds-react";
import { TilsagnDetaljer } from "../tilsagn/TilsagnDetaljer";
import { ArrangorflateTilsagnDto } from "@arrangor-utbetalinger/api-client";

export function TilgjengeligeTilsagn({ tilsagn }: { tilsagn: ArrangorflateTilsagnDto[] }) {
  return (
    <Box>
      <Heading level="3" size="small" spacing>
        Tilgjengelige tilsagn
      </Heading>
      <InlineMessage status="info">
        Under vises informasjon om antatt forbruk. Hva som blir utbetalt avhenger imidlertid av
        faktisk forbruk i perioden.
      </InlineMessage>
      <HGrid align="start" columns="1fr" gap="space-16" maxWidth="max-content">
        {tilsagn.map((tilsagn) => (
          <TilsagnDetaljer key={tilsagn.id} tilsagn={tilsagn} minimal />
        ))}
      </HGrid>
    </Box>
  );
}
