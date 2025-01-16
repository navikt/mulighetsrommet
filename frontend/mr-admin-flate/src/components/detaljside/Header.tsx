import { ReactNode } from "react";

interface Props {
  children: ReactNode;
}

export function Header({ children }: Props) {
  return (
    <div className="bg-white p-[1rem 0.5rem 0.5rem]">
      <div className="m-auto pl-2 gap-6 items-center flex">{children}</div>
    </div>
  );
}
