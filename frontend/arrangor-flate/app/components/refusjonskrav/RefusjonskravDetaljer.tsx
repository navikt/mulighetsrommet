import { Box, Heading } from "@navikt/ds-react";
import { Separator } from "~/components/Separator";
import { RefusjonDetaljer } from "~/components/refusjonskrav/RefusjonDetaljer";
import { ArrangorflateTilsagn, RefusjonKravAft } from "@mr/api-client-v2";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { GenerelleDetaljer } from "~/components/refusjonskrav/GenerelleDetaljer";

interface Props {
  krav: RefusjonKravAft;
  tilsagn: ArrangorflateTilsagn[];
}

export function RefusjonskravDetaljer({ krav, tilsagn }: Props) {
  return (
    <>
      <GenerelleDetaljer krav={krav} />
      <Separator />
      <Heading size="medium">Tilsagnsdetaljer</Heading>
      {tilsagn.map((t) => (
        <Box
          padding="2"
          key={t.id}
          borderWidth="1"
          borderColor="border-subtle"
          borderRadius="medium"
        >
          <TilsagnDetaljer tilsagn={t} />
        </Box>
      ))}
      <Separator />
      <RefusjonDetaljer krav={krav} />
    </>
  );
}
