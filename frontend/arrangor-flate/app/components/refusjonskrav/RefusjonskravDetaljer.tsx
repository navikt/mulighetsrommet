import { Box, Heading } from "@navikt/ds-react";
import { Separator } from "~/components/Separator";
import { Refusjonskrav } from "~/domene/domene";
import { RefusjonDetaljer } from "~/components/refusjonskrav/RefusjonDetaljer";
import { ArrangorflateTilsagn } from "@mr/api-client";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { GenerelleDetaljer } from "~/components/refusjonskrav/GenerelleDetaljer";

interface Props {
  krav: Refusjonskrav;
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
