import { Heading, HeadingProps, HStack, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";

export interface Definition {
  key: string;
  value: string | ReactNode;
}

interface Props {
  title?: string;
  className?: string;
  definitions: Definition[];
  headingLevel?: HeadingProps["level"];
}

export function Definisjonsliste({ title, definitions, className, headingLevel }: Props) {
  const headingSize = headingLevel === "4" ? "small" : "medium";
  return (
    <VStack gap="3" className={className}>
      {title && (
        <Heading size={headingSize} level={headingLevel}>
          {title}
        </Heading>
      )}
      <dl className="flex flex-col gap-1.5">
        {definitions.map((definition, index) => (
          <HStack gap="2" align="center" key={index}>
            <dt>{definition.key}:</dt>
            <dd className="font-bold text-right">{definition.value}</dd>
          </HStack>
        ))}
      </dl>
    </VStack>
  );
}
