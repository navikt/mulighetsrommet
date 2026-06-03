import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { tekster } from "~/tekster";
import { Box, VStack } from "@navikt/ds-react";
import { pathTo, useIdFromUrl } from "~/utils/navigation";
import { PageHeading } from "~/components/common/PageHeading";
import { useArrangorflateTilsagn } from "~/hooks/useArrangorflateTilsagn";

export default function TilsagnDetaljerPage() {
  const id = useIdFromUrl();
  const { data: tilsagn } = useArrangorflateTilsagn(id);

  return (
    <Box
      background="default"
      paddingInline="space-32"
      paddingBlock="space-32 space-64"
      borderRadius="8"
    >
      <VStack gap="space-16">
        <PageHeading
          title={tekster.bokmal.tilsagn.detaljer.headingTitle}
          tilbakeLenke={{
            navn: tekster.bokmal.tilsagn.detaljer.tilbakeLenke,
            url: pathTo.tilsagnOversikt,
          }}
        />
        <TilsagnDetaljer tilsagn={tilsagn} />
      </VStack>
    </Box>
  );
}
