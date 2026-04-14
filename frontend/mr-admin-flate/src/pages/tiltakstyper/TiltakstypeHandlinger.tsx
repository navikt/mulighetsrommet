import { Handlinger } from "@/components/handlinger/Handlinger";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { ActionMenu } from "@navikt/ds-react";
import { useNavigate } from "react-router";
import { useGetTiltakstypeIdFromUrlOrThrow } from "@/hooks/useGetTiltakstypeIdFromUrl";

export function TiltakstypeHandlinger() {
  const navigate = useNavigate();
  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();

  return (
    <KnapperadContainer>
      <Handlinger>
        <ActionMenu.Item
          onClick={() => navigate(`/tiltakstyper/${tiltakstypeId}/redaksjonelt-innhold/rediger`)}
        >
          Rediger informasjon for veiledere
        </ActionMenu.Item>
      </Handlinger>
    </KnapperadContainer>
  );
}
