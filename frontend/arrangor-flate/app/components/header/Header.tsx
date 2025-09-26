import { Link } from "react-router";
import { ArrangorflateArrangor } from "api-client";
import { Heading, HStack } from "@navikt/ds-react";
import { HeaderIcon } from "./HeaderIcon";
import { Arrangorvelger } from "../arrangorvelger/Arrangorvelger";

interface Props {
  arrangorer: ArrangorflateArrangor[];
}

export function Header({ arrangorer }: Props) {
  return (
    <header className="border-b-4 border-red-100">
      <HStack justify="space-between" padding="8" className="max-w-[1920px] w-[90%] m-auto">
        <HStack gap="8">
          <HeaderIcon />
          <Heading size="xlarge">
            <Link to="/" className="text-text-default no-underline">
              Utbetalinger til tiltaksarrangør
            </Link>
          </Heading>
        </HStack>
        <Arrangorvelger arrangorer={arrangorer} />
      </HStack>
    </header>
  );
}
