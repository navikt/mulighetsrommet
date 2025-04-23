import { Link } from "react-router";
import { Arrangorvelger } from "./arrangorvelger/Arrangorvelger";
import { Arrangor } from "api-client";
import { HStack } from "@navikt/ds-react";

interface Props {
  arrangorer: Arrangor[];
}

export function Header({ arrangorer }: Props) {
  return (
    <header className="bg-blue-100">
      <HStack justify="space-between" padding="12" className="mx-24">
        <Link className="text-gray-900 font-bold text-4xl no-underline" to="/">
          Utbetalinger
        </Link>
        <Arrangorvelger arrangorer={arrangorer} />
      </HStack>
    </header>
  );
}
