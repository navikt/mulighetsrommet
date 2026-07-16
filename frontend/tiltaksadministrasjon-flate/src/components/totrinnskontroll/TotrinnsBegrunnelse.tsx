import {
  MetadataFritekstfelt,
  MetadataVStack,
} from "@mr/frontend-common/components/datadriven/Metadata";
import { Heading } from "@navikt/ds-react";

interface TotrinnsBegrunnelseProps {
  title: string;
  aarsaker: string[];
  forklaring?: string | null;
}

export function TotrinnsBegrunnelse({ title, aarsaker, forklaring }: TotrinnsBegrunnelseProps) {
  if (aarsaker.length === 0 && !forklaring) {
    return null;
  }

  return (
    <>
      <Heading level="4" spacing size="small">
        {title}
      </Heading>
      <MetadataVStack label="Årsaker" value={aarsaker.join(", ")} />
      <MetadataFritekstfelt label="Forklaring" value={forklaring} />
    </>
  );
}
