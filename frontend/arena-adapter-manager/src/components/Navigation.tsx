import snoop from "../images/snoop.gif";
import { Box, Heading, HStack, Spacer } from "@navikt/ds-react";
import { Link } from "react-router";

function Navigation() {
  return (
    <HStack
      width="100%"
      gap="space-32"
      paddingInline="space-80"
      paddingBlock="space-40"
      style={{ backgroundColor: "Var(--a-purple-500)" }}
    >
      <Box width="fit-content">
        <Heading size="xlarge" spacing>
          <Link style={{ textDecoration: "none", color: "white" }} to="/">
            MAAM
          </Link>
        </Heading>
        <Heading style={{ color: "white" }} size="xsmall">
          mulighetsrommet-arena-adapter-manager
        </Heading>
      </Box>
      <Spacer />
      <img width="130px" src={snoop} alt="snoop dogg" style={{ objectFit: "cover" }} />
    </HStack>
  );
}

export default Navigation;
