import { Link } from "react-router";
import { Arrangorvelger } from "./arrangorvelger/Arrangorvelger";
import { Arrangor } from "api-client";
import { Heading, HStack } from "@navikt/ds-react";
import { HeaderIcon } from "./HeaderIcon";

interface Props {
  arrangorer: Arrangor[];
}

export function Header({ arrangorer }: Props) {
  return (
    <header className="border-b-4 border-red-100">
      <HStack justify="space-between" padding="8" className="max-w-[2500px] w-[90%] m-auto">
        <HStack gap="8">
          <HeaderIcon />
          <Heading
            size="xlarge"
            textColor="default"
            as={Link}
            className="text-text-default no-underline"
            to="/"
          >
            Tilsagn og utbetalinger
          </Heading>
        </HStack>
        <Arrangorvelger arrangorer={arrangorer} />
      </HStack>
    </header>
  );
}
