import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { ActionMenu, Button } from "@navikt/ds-react";
import { Link } from "react-router";

export function Behandlingsoversikt() {
  return (
    <KnapperadContainer>
      <ActionMenu>
        <ActionMenu.Trigger>
          <Button size="small" variant="secondary">
            Handlinger
          </Button>
        </ActionMenu.Trigger>
        <ActionMenu.Content>
          <ActionMenu.Item as={Link} to={`opprett-behandling`}>
            Opprett tilskuddsbehandling
          </ActionMenu.Item>
        </ActionMenu.Content>
      </ActionMenu>
    </KnapperadContainer>
  );
}
