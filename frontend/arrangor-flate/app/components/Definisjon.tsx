import { Heading } from "@navikt/ds-react";
import React from "react";
import { Definition } from "~/components/Definisjonsliste";

export function Definisjon({
  label,
  children,
  className,
}: {
  label: string;
  className?: string;
  children: React.ReactNode;
}) {
  const styles = "flex justify-between";
  return (
    <div className={className ? `${styles} ${className}` : styles} key={label}>
      <dt>{label}:</dt>
      <dd className="font-bold text-right">{children}</dd>
    </div>
  );
}
