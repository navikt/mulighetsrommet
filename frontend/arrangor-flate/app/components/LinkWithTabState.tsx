import { Link } from "@navikt/ds-react";
import { Link as ReactRouterLink, LinkProps, useSearchParams } from "react-router";
interface Props extends LinkProps {
  tabKey?: string;
}

export function LinkWithTabState({ to, children, tabKey = "forside-tab", ...props }: Props) {
  const [searchParams] = useSearchParams();
  const currentTab = searchParams.get(tabKey);
  const linkTo = currentTab ? `${to}?${tabKey}=${encodeURIComponent(currentTab)}` : to;

  return (
    <Link as={ReactRouterLink} to={linkTo} {...props}>
      {children}
    </Link>
  );
}
