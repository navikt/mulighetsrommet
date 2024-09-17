import { Heading } from "@navikt/ds-react";
import React from "react";

interface Definition {
  key: string;
  value: string;
}

interface Props {
  title?: string;
  definitions: Definition[];
}

export function Definisjonsliste({ title, definitions }: Props) {
  return (
    <div>
      {title && (
        <Heading className="mb-2" size="medium" level="2">
          {title}
        </Heading>
      )}
      <dl className="max-w-[50%] flex flex-col gap-2">
        {definitions.map((definition, index) => (
          <div className="flex justify-between" key={index}>
            <dt>{definition.key}:</dt>
            <dd className="font-bold">{definition.value}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}
