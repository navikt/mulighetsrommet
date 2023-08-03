import { Button } from "@navikt/ds-react";

interface Props {
  dataTestId?: string;
  setAvbrytModalOpen: (state: boolean) => void;
  type: "avtale" | "gjennomføring";
}
export function AvbrytAvtaleGjennomforingKnapp({
  dataTestId,
  setAvbrytModalOpen,
  type,
}: Props) {
  return (
    <Button
      variant="danger"
      onClick={() => setAvbrytModalOpen(true)}
      data-testid={dataTestId}
      type="button"
    >
      Avbryt {type}
    </Button>
  );
}
