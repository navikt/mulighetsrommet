import { BodyShort, Box, Heading, VStack } from "@navikt/ds-react";
import { Link } from "react-router";
import { kebabCase } from "../../../../../frontend-common/utils/TestUtils";
import { ReactNode } from "react";

export interface ForsideKortProps {
  navn: string;
  ikon: ReactNode;
  url: string;
  apneINyTab?: boolean;
  tekst?: string;
}

export function Forsidekort({ navn, ikon, url, tekst, apneINyTab = false }: ForsideKortProps) {
  return (
    <Box
      as={Link}
      background="bg-default"
      borderRadius="4"
      key={url}
      className="text-text-default shadow-md hover:shadow-lg transition-all duration-150 ease-in-out"
      to={url}
      {...(apneINyTab ? { target: "_blank", rel: "noopener noreferrer" } : {})}
      data-testid={`forsidekort-${kebabCase(navn)}`}
    >
      <VStack align="center" gap="4" padding="12">
        <span className="flex justify-center items-center w-[100px] h-[100px] rounded-full mx-auto">
          <div className="[&>svg]:w-16 [&>svg]:h-16">{ikon}</div>
        </span>
        <Heading align="center" size="medium" level="3">
          {navn}
        </Heading>
        {tekst ? (
          <BodyShort align="center" className="text-gray-600">
            {tekst}
          </BodyShort>
        ) : null}
      </VStack>
    </Box>
  );
}
