import { Button } from "@navikt/ds-react";

interface Props {
  dataTestId?: string;
  onClick: () => void;
  type: "avtale" | "gjennomføring";
}
export function AvbrytAvtaleGjennomforingKnapp({
  dataTestId,
  onClick,
  type,
}: Props) {
  return (
    <Button
      variant="danger"
      onClick={onClick}
      data-testid={dataTestId}
      type="button"
    >
      Avbryt {type}
    </Button>
  );
}
