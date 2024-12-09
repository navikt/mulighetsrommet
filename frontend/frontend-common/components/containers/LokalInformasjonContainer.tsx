import { PropsWithChildren } from "react";
import { Box } from "@navikt/ds-react";

export function LokalInformasjonContainer(props: PropsWithChildren) {
  return (
    <Box background="bg-subtle" padding={"5"}>
      {props.children}
    </Box>
  );
}
