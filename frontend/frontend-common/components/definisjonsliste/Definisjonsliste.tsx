import { ReactNode } from "react";
import { VStack, Heading, HeadingProps, Label, BodyShort, HGrid } from "@navikt/ds-react";

export interface Definition {
  key: string;
  value: string | ReactNode;
}

export function Definisjonsliste({
  title,
  definitions,
  columns = 2,
  headingLevel = "3",
}: {
  title?: string;
  definitions: Definition[];
  columns?: 1 | 2;
  headingLevel?: HeadingProps['level']
}) {
  return (
    <VStack gap="3">
      {title && (
        <Heading size="small" level={headingLevel}>
          {title}
        </Heading>
      )}
      <HGrid as="dl" columns={columns} gap="6">
        {definitions.map((definition, index) => (
          <VStack gap="2" key={index}>
            <Label as="dt">{definition.key}</Label>
            <BodyShort as="dd">{definition.value}</BodyShort>
          </VStack>
        ))}
      </HGrid>
    </VStack>
  );
}
