import { Heading, HeadingProps, HStack, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import { formaterNOK, formaterTall } from "@mr/frontend-common/utils/utils";
import { LabeledDataElement } from "@api-client";
import { getDataElement } from "@mr/frontend-common";

export interface Definition {
  key: string;
  value: string | ReactNode;
  format?: DefinitionFormat | null;
}

export type DefinitionFormat = "NOK" | "NUMBER";

interface Props2 {
  title?: string;
  className?: string;
  definitions: LabeledDataElement[];
  headingLevel?: HeadingProps["level"];
}

export function Definisjonsliste2({ title, definitions, className, headingLevel }: Props2) {
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
            <dt>{definition.label}:</dt>
            <dd className="font-bold text-right">
              {definition.value ? getDataElement(definition.value) : null}
            </dd>
          </HStack>
        ))}
      </dl>
    </VStack>
  );
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
            <dd className="font-bold text-right">{getFormattedValue(definition)}</dd>
          </HStack>
        ))}
      </dl>
    </VStack>
  );
}

function getFormattedValue(definition: Definition) {
  return definition.format && typeof definition.value === "string"
    ? formatValue(definition.value, definition.format)
    : definition.value;
}

function formatValue(value: string, format: DefinitionFormat) {
  switch (format) {
    case "NOK":
      return formaterNOK(Number(value));
    case "NUMBER":
      return formaterTall(Number(value));
  }
}
