import {
  Container,
  LinkBox,
  LinkOverlay,
  Heading,
  Box,
} from "@chakra-ui/react";

export const Layout = ({ children }: React.PropsWithChildren) => (
  <Box as="main" w="100%">
    <Container mt="8" maxW="container.xl">
      <LinkBox mb="8" w="fit-content">
        <LinkOverlay
          _hover={{ color: "pink.500" }}
          href="https://youtu.be/RJHctyXPmkg?t=5"
        >
          <Heading size="4xl">MAAM</Heading>
        </LinkOverlay>
        <Heading size="xs">mulighetsrommet-arena-adapter-manager</Heading>
      </LinkBox>
      {children}
    </Container>
  </Box>
);
