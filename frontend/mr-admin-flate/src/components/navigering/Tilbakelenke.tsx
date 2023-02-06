import { Link } from "@navikt/ds-react";
import React from "react";

export function Tilbakelenke({
  children,
  to,
}: {
  children: React.ReactNode;
  to: string;
}) {
  return (
    <Link style={{ marginBottom: "1rem" }} href={to} data-testid="tilbakelenke">
      {children}
    </Link>
  );
}
