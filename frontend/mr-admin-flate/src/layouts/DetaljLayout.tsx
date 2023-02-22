import { ReactNode } from "react";

interface Props {
  children: ReactNode;
}

export function DetaljLayout({ children }: Props) {
  return <div style={{ padding: "0.5rem" }}>{children}</div>;
}
