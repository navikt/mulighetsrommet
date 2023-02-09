import { ReactNode } from "react";

export function MainContent({ children }: { children: ReactNode }) {
  return <main style={{ padding: "1rem" }}>{children}</main>;
}
