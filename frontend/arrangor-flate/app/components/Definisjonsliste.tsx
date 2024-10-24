import { Heading } from "@navikt/ds-react";
import React from "react";

export interface Definition {
  key: string;
  value: string;
}

interface Props {
  title?: string;
  className?: string;
  definitions: Definition[];
}

export function Definisjonsliste({ title, definitions, className }: Props) {
  return (
    <div className={className}>
      {title && (
        <Heading className="mb-2" size="medium" level="2">
          {title}
        </Heading>
      )}
      <dl className="flex flex-col gap-2">
        {definitions.map((definition, index) => (
          <div className="flex justify-between" key={index}>
            <dt>{definition.key}:</dt>
            <dd className="font-bold text-right">{definition.value}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}
