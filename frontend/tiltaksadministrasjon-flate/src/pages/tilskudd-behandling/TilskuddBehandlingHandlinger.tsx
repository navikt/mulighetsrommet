import { useTilskuddBehandling } from "@/api/tilskudd-behandling/useTilskuddBehandling";
import { useVedtaksbrevPdfBlob } from "@/api/tilskudd-behandling/useVedtaksbrevPdfBlob";
import { Endringshistorikk } from "@/components/endringshistorikk/Endringshistorikk";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { VedtaksbrevPdfModal } from "@/components/tilskudd-behandling/VedtaksbrevPdfModal";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { erTilBeslutning } from "@/utils/totrinnskontroll";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { FilePdfIcon, PencilFillIcon } from "@navikt/aksel-icons";
import { Button } from "@navikt/ds-react";
import {
  EndringshistorikkType,
  TilskuddBehandlingHandling,
} from "@tiltaksadministrasjon/api-client";
import { useState } from "react";

export function TilskuddBehandlingHandlinger({
  tilskuddBehandlingId,
}: {
  tilskuddBehandlingId: string;
}) {
  const {
    data: { behandling, handlinger, opprettelse },
  } = useTilskuddBehandling(tilskuddBehandlingId);

  const [pdfPreviewOpen, setPdfPreviewOpen] = useState(false);
  const {
    data: pdfBlob,
    isLoading: pdfIsLoading,
    isError: pdfIsError,
  } = useVedtaksbrevPdfBlob(tilskuddBehandlingId, pdfPreviewOpen);

  return (
    <>
      <KnapperadContainer>
        {erTilBeslutning(opprettelse) && (
          <Button
            variant="tertiary"
            size="small"
            onClick={() => setPdfPreviewOpen(true)}
            icon={<FilePdfIcon aria-hidden />}
          >
            Vis vedtaksbrev
          </Button>
        )}
        <Endringshistorikk id={behandling.id} type={EndringshistorikkType.TILSKUDD_BEHANDLING} />
        <Handlinger
          handlinger={handlinger}
          grupper={[
            {
              items: [
                {
                  label: "Rediger tilskuddsbehandling",
                  href: "rediger",
                  handling: TilskuddBehandlingHandling.REDIGER,
                  icon: <PencilFillIcon />,
                },
              ],
            },
          ]}
        />
      </KnapperadContainer>
      <Separator />
      <VedtaksbrevPdfModal
        blob={pdfBlob}
        isLoading={pdfIsLoading}
        isError={pdfIsError}
        open={pdfPreviewOpen}
        onClose={() => setPdfPreviewOpen(false)}
      />
    </>
  );
}
