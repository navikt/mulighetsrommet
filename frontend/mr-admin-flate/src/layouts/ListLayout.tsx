import { ReactNode } from "react";

interface Props {
  children: ReactNode;
}

export function ListLayout({ children }: Props) {
  return (
    <div style={{ padding: "0.5rem", maxWidth: "1920px", margin: "1rem auto" }}>
      {children}
    </div>
  );
}
