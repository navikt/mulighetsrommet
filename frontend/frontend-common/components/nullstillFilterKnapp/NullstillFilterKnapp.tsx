import { Button } from "@navikt/ds-react";
import style from "./NullstillFilterKnapp.module.scss";

interface Props {
  onClick: () => void;
}

export const NullstillFilterKnapp = ({ onClick }: Props) => {
  return (
    <Button
      type="button"
      size="small"
      className={style.nullstill_filter}
      variant="tertiary"
      onClick={onClick}
      data-testid="knapp_nullstill-filter"
    >
      Nullstill filter
    </Button>
  );
};
