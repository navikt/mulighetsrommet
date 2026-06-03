import { Button } from "@navikt/ds-react";

interface Props {
  onClick: () => void;
  knappetekst: string | React.ReactNode;
}

export function KontaktpersonButton({ onClick, knappetekst }: Props) {
  return (
    <Button
      className="mt-2 ml-auto"
      size="small"
      type="button"
      variant="tertiary"
      onClick={() => onClick()}
    >
      {knappetekst}
    </Button>
  );
}
