import { Lenke } from "mulighetsrommet-frontend-common/components/Lenke";
import styles from "./Tilbakeknapp.module.scss";
import { BodyShort } from "@navikt/ds-react";
import { ChevronLeftIcon } from "@navikt/aksel-icons";

interface TilbakeknappProps {
  tilbakelenke: string;
  tekst?: string;
}

export const Tilbakeknapp = ({ tilbakelenke, tekst = "Tilbake" }: TilbakeknappProps) => {
  return (
    <Lenke className={styles.tilbakeknapp} to={tilbakelenke}>
      <ChevronLeftIcon aria-label="Tilbakeknapp" />
      <BodyShort size="small">{tekst}</BodyShort>
    </Lenke>
  );
};
