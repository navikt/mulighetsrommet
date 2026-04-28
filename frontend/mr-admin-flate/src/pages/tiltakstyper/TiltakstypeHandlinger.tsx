import { Handlinger } from "@/components/handlinger/Handlinger";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { ActionMenu } from "@navikt/ds-react";
import { useNavigate } from "react-router";
import { useGetTiltakstypeIdFromUrlOrThrow } from "@/hooks/useGetTiltakstypeIdFromUrl";
import { useTiltakstypeHandlinger } from "@/api/tiltakstyper/useTiltakstypeHandlinger";
import { TiltakstypeHandling } from "@tiltaksadministrasjon/api-client";

export function TiltakstypeHandlinger() {
  const navigate = useNavigate();
  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();
  const handlinger = useTiltakstypeHandlinger();

  return (
    <KnapperadContainer>
      <Handlinger>
        {handlinger.includes(TiltakstypeHandling.REDIGER_VEILEDERINFO) && (
          <ActionMenu.Item
            onClick={() => navigate(`/tiltakstyper/${tiltakstypeId}/redaksjonelt-innhold/rediger`)}
          >
            Rediger informasjon for veiledere
          </ActionMenu.Item>
        )}
        {handlinger.includes(TiltakstypeHandling.REDIGER_DELTAKERINFO) && (
          <ActionMenu.Item
            onClick={() => navigate(`/tiltakstyper/${tiltakstypeId}/deltaker-registrering/rediger`)}
          >
            Rediger informasjon for deltakere
          </ActionMenu.Item>
        )}
      </Handlinger>
    </KnapperadContainer>
  );
}
