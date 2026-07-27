import { Accordion, Box, HStack } from "@navikt/ds-react";
import React from "react";

interface Props extends React.PropsWithChildren {
  tittel: string;
  antallValgteFilter?: number;
  tilleggsinformasjon?: React.ReactNode;
  open: boolean;
  onClick: () => void;
}
export function FilterAccordion({
  antallValgteFilter,
  tittel,
  tilleggsinformasjon,
  children,
  onClick,
  open,
}: Props) {
  return (
    <Accordion.Item open={open}>
      <Accordion.Header onClick={onClick} className="group">
        <HStack align="center" justify="space-between" gap="space-16">
          {tittel}
          {tilleggsinformasjon && tilleggsinformasjon}
          {antallValgteFilter && antallValgteFilter !== 0 ? (
            <Box
              borderRadius="8"
              paddingInline="space-8"
              background="accent-moderateA"
              className="group-hover:bg-ax-bg-accent-strong-hover group-hover:text-ax-text-neutral-contrast self-end"
            >
              {antallValgteFilter}
            </Box>
          ) : null}
        </HStack>
      </Accordion.Header>
      <Accordion.Content>{children}</Accordion.Content>
    </Accordion.Item>
  );
}
