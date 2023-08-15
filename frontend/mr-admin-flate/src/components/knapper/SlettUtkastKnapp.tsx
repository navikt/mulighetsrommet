import { Button } from "@navikt/ds-react";

interface Props {
  setSletteModal: () => void;
  dataTestId?: string;
  size?: "small" | "medium" | "xsmall";
}
export function SlettUtkastKnapp({ setSletteModal, dataTestId, size }: Props) {
  return (
    <Button
      variant="danger"
      type="button"
      onClick={setSletteModal}
      size={size}
      data-testid={dataTestId}
    >
      Slett utkast
    </Button>
  );
}
