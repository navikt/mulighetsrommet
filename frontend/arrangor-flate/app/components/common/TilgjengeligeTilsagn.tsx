import { Box, Heading, BodyShort, HGrid } from "@navikt/ds-react";
import { TilsagnDetaljer } from "../tilsagn/TilsagnDetaljer";
import { UtbetalingManglendeTilsagnAlert } from "../utbetaling/UtbetalingManglendeTilsagnAlert";
import { ArrangorflateTilsagnDto } from "api-client/types.gen";

export function TilgjengeligeTilsagn({ tilsagn }: { tilsagn: ArrangorflateTilsagnDto[] }) {
  return (
    <Box>
      <Heading level="3" size="small" spacing>
        Tilgjengelige tilsagn
      </Heading>
      <BodyShort size="small" textColor="subtle" spacing>
        Under vises informasjon om antatt forbruk.
        <br />
        Hva som blir utbetalt avhenger imidlertid av faktisk forbruk i perioden.
      </BodyShort>
      <HGrid align="start" columns="1fr" gap="space-16" maxWidth="max-content">
        {tilsagn.length < 1 ? (
          <UtbetalingManglendeTilsagnAlert />
        ) : (
          <>
            {tilsagn.map((tilsagn) => (
              <TilsagnDetaljer key={tilsagn.id} tilsagn={tilsagn} minimal />
            ))}
          </>
        )}
      </HGrid>
    </Box>
  );
}
