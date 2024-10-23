import { Heading } from "@navikt/ds-react";
import React from "react";
import { Definition } from "~/components/Definisjonsliste";

export function Definisjon({
  key,
  value,
  className,
}: Definition & { className?: string; value: React.ReactNode }) {
  const styles = "flex justify-between";
  return (
    <div className={className ? `${styles} ${className}` : styles} key={key}>
      <dt>{key}:</dt>
      <dd className="font-bold text-right">{value}</dd>
    </div>
  );
}
