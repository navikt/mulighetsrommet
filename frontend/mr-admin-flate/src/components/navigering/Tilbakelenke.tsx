import { Link } from "@navikt/ds-react";
import React from "react";
import { useNavigate } from "react-router-dom";

interface TilbakelenkeProps {
  children: React.ReactNode;
  dataTestId?: string;
}

export function Tilbakelenke({ dataTestId, children }: TilbakelenkeProps) {
  const navigate = useNavigate();
  const navigerTilbake = () => navigate(-1);

  return (
    <Link
      style={{ marginBottom: "1rem" }}
      href="#"
      onClick={navigerTilbake}
      data-testid={dataTestId}
    >
      {children}
    </Link>
  );
}
