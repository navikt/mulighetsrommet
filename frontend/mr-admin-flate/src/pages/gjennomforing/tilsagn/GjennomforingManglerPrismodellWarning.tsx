import { InfoCard } from "@navikt/ds-react";

export function GjennomforingManglerPrismodellWarning() {
  return (
    <InfoCard data-color="warning">
      <InfoCard.Header>
        <InfoCard.Title>Tilsagn kan ikke opprettes</InfoCard.Title>
      </InfoCard.Header>
      <InfoCard.Content>
        Gjennomføringen mangler prismodell og tilsagn kan derfor ikke opprettes.
      </InfoCard.Content>
    </InfoCard>
  );
}
