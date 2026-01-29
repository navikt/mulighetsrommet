import { Link, useNavigation } from "react-router";
import { Box, Heading, HStack, Loader } from "@navikt/ds-react";
import { HeaderIcon } from "./HeaderIcon";

export function Header() {
  const navigation = useNavigation();

  return (
    <Box
      as="header"
      marginBlock="0 8"
      background="bg-default"
      borderWidth="0 0 4 0"
      className="border-red-100"
    >
      <HStack gap="8" padding="8">
        <HeaderIcon />
        <Heading size="xlarge" level="1">
          <Link to="/" className="text-text-default no-underline">
            Utbetalinger til tiltaksarrangør
          </Link>
        </Heading>
        {navigation.state === "loading" && <Loader />}
      </HStack>
    </Box>
  );
}
