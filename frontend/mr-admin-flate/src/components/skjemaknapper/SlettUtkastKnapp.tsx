import { Button } from "@navikt/ds-react";

interface Props {
  setSlettemodal: () => void;
  dataTestId?: string;
  size?: "small" | "medium" | "xsmall";
  disabled: boolean;
}

export function SlettUtkastKnapp({
  setSlettemodal,
  dataTestId,
  size,
  disabled,
}: Props) {
  return (
    <Button
      variant="danger"
      type="button"
      onClick={setSlettemodal}
      size={size}
      data-testid={dataTestId}
      disabled={disabled}
    >
      Slett utkast
    </Button>
  );
}
