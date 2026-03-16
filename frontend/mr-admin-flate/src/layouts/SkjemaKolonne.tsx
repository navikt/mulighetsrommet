import { VStack } from "@navikt/ds-react";
import { PropsWithChildren } from "react";

export function SkjemaKolonne(props: PropsWithChildren) {
  return <VStack gap="space-12">{props.children}</VStack>;
}
