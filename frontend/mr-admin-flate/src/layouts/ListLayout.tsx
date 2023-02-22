import { ReactNode } from "react";

interface Props {
  children: ReactNode;
}

export function ListLayout({ children }: Props) {
  return <div style={{ padding: "0.5rem" }}>{children}</div>;
}
