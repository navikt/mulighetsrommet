import { Link } from "@navikt/ds-react";
import React from "react";
import { useNavigate } from "react-router-dom";
import { ChevronLeftIcon } from "@navikt/aksel-icons";
import styles from "./Tilbakelenke.module.scss";

export function Tilbakelenke({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate();
  const navigerTilbake = () => {
    navigate(-1);
  };

  return (
    <Link
      className={styles.tilbakelenke}
      href="#"
      onClick={navigerTilbake}
      data-testid="tilbakelenke"
    >
      <ChevronLeftIcon aria-label="Tilbakeknapp" />
      {children}
    </Link>
  );
}
