import { ReactNode } from "react";
import { BodyShort, Heading, HeadingProps, HGrid, Label, VStack } from "@navikt/ds-react";
import { MetadataFritekstfelt } from "../datadriven/Metadata";

export interface Definition {
  key: string;
  value: string | ReactNode;
  spanColumns?: boolean;
  fritekst?: boolean;
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
  headingLevel?: HeadingProps["level"];
}) {
  return (
    <VStack gap="space-12">
      {title && (
        <Heading size="small" level={headingLevel}>
          {title}
        </Heading>
      )}
      <HGrid as="dl" columns={columns} gap="space-24">
        {definitions.map((definition, index) => (
          <VStack
            gap="space-8"
            key={index}
            style={definition.spanColumns ? { gridColumn: "span 2" } : undefined}
          >
            {definition.fritekst ? (
              <MetadataFritekstfelt label={definition.key} value={definition.value} />
            ) : (
              <>
                <Label as="dt">{definition.key}</Label>
                <BodyShort as="dd">{definition.value ?? "-"}</BodyShort>
              </>
            )}
          </VStack>
        ))}
      </HGrid>
    </VStack>
  );
}
