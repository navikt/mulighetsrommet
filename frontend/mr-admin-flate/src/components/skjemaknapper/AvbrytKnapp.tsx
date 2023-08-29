import { Button } from "@navikt/ds-react";
import { useNavigate } from "react-router-dom";

interface Props {
  setSlettemodal: () => void;
  dataTestId?: string;
  size?: "small" | "medium" | "xsmall";
  dirtyForm: boolean;
}

export function AvbrytKnapp({
  setSlettemodal,
  dataTestId,
  size,
  dirtyForm,
}: Props) {
  const navigate = useNavigate();

  const navigerTilbake = () => {
    navigate(-1);
  };

  return (
    <Button
      variant="tertiary"
      type="button"
      onClick={dirtyForm ? setSlettemodal : navigerTilbake}
      size={size}
      data-testid={dataTestId}
    >
      Avbryt
    </Button>
  );
}
