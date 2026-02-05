import { PropsWithChildren } from "react";
import { Box } from "@navikt/ds-react";

export function LokalInformasjonContainer(props: PropsWithChildren) {
  return (
    <Box background="neutral-soft" padding={"space-20"}>
      {props.children}
    </Box>
  );
}
