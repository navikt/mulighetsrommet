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
    <Box background="bg-default" paddingInline="8" paddingBlock="8 16" borderRadius="large">
      <VStack gap="4">
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
