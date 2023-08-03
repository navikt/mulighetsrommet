import { Button } from "@navikt/ds-react";

interface Props {
  setSletteModal: (state: boolean) => void;
}
export function SlettUtkastKnapp({ setSletteModal }: Props) {
  return (
    <Button variant="danger" type="button" onClick={() => setSletteModal(true)}>
      Slett utkast
    </Button>
  );
}
