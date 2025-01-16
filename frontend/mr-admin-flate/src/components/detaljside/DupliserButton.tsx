import { Button } from "@navikt/ds-react";
import { LayersPlusIcon } from "@navikt/aksel-icons";

interface Props {
  onClick: () => void;
  title: string;
}

export function DupliserButton({ onClick, title }: Props) {
  return (
    <Button title={title} className="bg-[var(--a-surface-action)] max-h-8" onClick={onClick}>
      <div className="flex items-start justify-between text-base gap-2">
        <LayersPlusIcon
          color="white"
          fontSize="1.5rem"
          aria-label="Ikon for duplisering av dokument"
        />
        Dupliser
      </div>
    </Button>
  );
}
