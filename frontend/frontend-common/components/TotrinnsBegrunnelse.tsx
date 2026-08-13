import { Heading, HeadingProps, VStack } from "@navikt/ds-react";
import { MetadataFritekstfelt, MetadataVStack } from "./datadriven/Metadata";

interface TotrinnsBegrunnelseProps {
  title: string;
  aarsaker: string[];
  forklaring?: string | null;
  headerSpacing?: boolean;
  size?: HeadingProps["size"];
}

export function TotrinnsBegrunnelse({
  title,
  aarsaker,
  forklaring,
  headerSpacing = true,
  size = "small",
}: TotrinnsBegrunnelseProps) {
  if (aarsaker.length === 0 && !forklaring) {
    return null;
  }

  return (
    <>
      <Heading level="4" spacing={headerSpacing} size={size}>
        {title}
      </Heading>
      <VStack gap="space-16">
        <MetadataVStack label="Årsaker" value={aarsaker.join(", ")} />
        <MetadataFritekstfelt label="Forklaring" value={forklaring} />
      </VStack>
    </>
  );
}
