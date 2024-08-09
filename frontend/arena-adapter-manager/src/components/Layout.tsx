import { Container, Box } from "@chakra-ui/react";
import { Outlet } from "react-router-dom";
import Navigation from "./Navigation";

export function Layout() {
  return (
    <Box as="main" w="100%">
      <Navigation />
      <Box w="inherit" py="8">
        <Container maxW="container.xl">
          <Outlet />
        </Container>
      </Box>
    </Box>
  );
}
