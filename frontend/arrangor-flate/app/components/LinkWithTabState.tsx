import { Link, LinkProps, useSearchParams } from "@remix-run/react";
interface Props extends LinkProps {
  tabKey?: string;
}

export function LinkWithTabState({ to, children, tabKey = "forside-tab", ...props }: Props) {
  const [searchParams] = useSearchParams();
  const currentTab = searchParams.get(tabKey);
  const linkTo = currentTab ? `${to}?${tabKey}=${encodeURIComponent(currentTab)}` : to;

  return (
    <Link to={linkTo} {...props}>
      {children}
    </Link>
  );
}
