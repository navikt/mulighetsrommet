import { Outlet, useLocation, useNavigate } from "react-router";
import Navigation from "./Navigation";
import { Box, Tabs, VStack } from "@navikt/ds-react";
import { CogIcon } from "@navikt/aksel-icons";

interface Props {
  apps: { name: string; path: string }[];
}

export function Layout({ apps }: Props) {
  const { pathname } = useLocation();
  const navigate = useNavigate();

  return (
    <Box as="main" width="100%">
      <Navigation />
      <Tabs value={pathname}>
        <Tabs.List>
          {apps.map((app) => (
            <Tabs.Tab
              key={app.name}
              value={app.path}
              label={app.name}
              icon={<CogIcon />}
              onClick={() => navigate(app.path)}
            />
          ))}
        </Tabs.List>
        <Tabs.Panel value={pathname}>
          <VStack gap="space-32" padding="space-32">
            <Outlet />
          </VStack>
        </Tabs.Panel>
      </Tabs>
    </Box>
  );
}
