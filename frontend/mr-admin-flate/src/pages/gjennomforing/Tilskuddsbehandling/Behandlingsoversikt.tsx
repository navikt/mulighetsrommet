import { Handlinger } from "@/components/handlinger/Handlinger";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { ActionMenu } from "@navikt/ds-react";
import { Link } from "react-router";

export function Behandlingsoversikt() {
  return (
    <KnapperadContainer>
      <Handlinger>
        <ActionMenu.Item as={Link} to={`opprett-behandling`}>
          Opprett tilskuddsbehandling
        </ActionMenu.Item>
      </Handlinger>
    </KnapperadContainer>
  );
}
