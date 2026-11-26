import { useTilskuddBehandling } from "@/api/tilskudd-behandling/useTilskuddBehandling";
import { fetchVedtaksbrevPdfBlob } from "@/api/tilskudd-behandling/useVedtaksbrevPdfBlob";
import { Endringshistorikk } from "@/components/endringshistorikk/Endringshistorikk";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { useOpenPdfInNewTab } from "@/hooks/useOpenPdfInNewTab";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { erTilBeslutning } from "@/utils/totrinnskontroll";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { FilePdfIcon, PencilFillIcon } from "@navikt/aksel-icons";
import { Button, InlineMessage } from "@navikt/ds-react";
import {
  EndringshistorikkType,
  TilskuddBehandlingHandling,
} from "@tiltaksadministrasjon/api-client";

export function TilskuddBehandlingHandlinger({
  tilskuddBehandlingId,
}: {
  tilskuddBehandlingId: string;
}) {
  const {
    data: { behandling, handlinger, opprettelse },
  } = useTilskuddBehandling(tilskuddBehandlingId);

  const { open, feilmelding } = useOpenPdfInNewTab();

  return (
    <>
      <KnapperadContainer>
        {erTilBeslutning(opprettelse) && (
          <Button
            variant="tertiary"
            size="small"
            onClick={() => open(() => fetchVedtaksbrevPdfBlob(tilskuddBehandlingId))}
            icon={<FilePdfIcon aria-hidden />}
          >
            Vis vedtaksbrev
          </Button>
        )}
        {feilmelding && (
          <InlineMessage status="error" size="small">
            {feilmelding}
          </InlineMessage>
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
    </>
  );
}
