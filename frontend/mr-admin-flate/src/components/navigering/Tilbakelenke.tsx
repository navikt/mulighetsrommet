import { Link } from "@navikt/ds-react";
import React from "react";
import { useNavigate } from "react-router-dom";

export function Tilbakelenke({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate();
  const navigerTilbake = () => navigate(-1);

  return (
    <Link
      style={{ marginBottom: "1rem" }}
      href="#"
      onClick={navigerTilbake}
      data-testid="tilbakelenke"
    >
      {children}
    </Link>
  );
}
