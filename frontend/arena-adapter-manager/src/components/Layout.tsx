import { Container, Box } from "@chakra-ui/react";
import { Outlet } from "react-router";
import Navigation from "./Navigation";

interface Props {
  apps: { name: string; path: string }[];
}

export function Layout({ apps }: Props) {
  return (
    <Box as="main" w="100%">
      <Navigation apps={apps} />
      <Box w="inherit" py="8">
        <Container maxW="container.xl">
          <Outlet />
        </Container>
      </Box>
    </Box>
  );
}
