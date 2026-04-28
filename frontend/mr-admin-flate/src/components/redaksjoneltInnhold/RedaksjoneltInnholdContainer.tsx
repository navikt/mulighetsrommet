import { PropsWithChildren } from "react";

interface Props {
  separator?: boolean;
}
export function RedaksjoneltInnholdContainer({
  separator = false,
  children,
}: PropsWithChildren<Props>) {
  return (
    <div className="max-w-[900px] flex flex-col gap-4">
      {children}
      {separator ? <hr /> : null}
    </div>
  );
}
